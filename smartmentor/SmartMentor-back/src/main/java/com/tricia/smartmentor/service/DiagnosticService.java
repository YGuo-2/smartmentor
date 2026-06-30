package com.tricia.smartmentor.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tricia.smartmentor.agent.AgentContext;
import com.tricia.smartmentor.agent.AgentResponse;
import com.tricia.smartmentor.agent.DiagnosticAgent;
import com.tricia.smartmentor.common.BusinessException;
import com.tricia.smartmentor.entity.AnswerRecord;
import com.tricia.smartmentor.entity.DiagnosticSession;
import com.tricia.smartmentor.entity.StudentProfile;
import com.tricia.smartmentor.repository.AnswerRecordRepository;
import com.tricia.smartmentor.repository.DiagnosticSessionRepository;
import com.tricia.smartmentor.repository.StudentProfileRepository;
import com.tricia.smartmentor.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DiagnosticService {

    private final DiagnosticSessionRepository diagnosticSessionRepository;
    private final AnswerRecordRepository answerRecordRepository;
    private final RedisUtil redisUtil;
    private final DiagnosticAgent diagnosticAgent;
    private final KnowledgeGraphService knowledgeGraphService;
    private final MasteryUpdateService masteryUpdateService;
    private final QuestionBankService questionBankService;
    private final StudentProfileRepository studentProfileRepository;
    private final LlmService llmService;
    private final ObjectMapper objectMapper;
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(4);

    private static final String DIAG_CACHE_PREFIX = "diag:session:";
    private static final String DIAG_QUESTIONS_PREFIX = "diag:questions:";
    private static final int DIAG_CACHE_MINUTES = 60;
    private static final int TOTAL_ESTIMATED_QUESTIONS = 8;

    public DiagnosticService(DiagnosticSessionRepository diagnosticSessionRepository,
                             AnswerRecordRepository answerRecordRepository,
                             RedisUtil redisUtil,
                             DiagnosticAgent diagnosticAgent,
                             KnowledgeGraphService knowledgeGraphService,
                             MasteryUpdateService masteryUpdateService,
                             QuestionBankService questionBankService,
                             StudentProfileRepository studentProfileRepository,
                             LlmService llmService,
                             ObjectMapper objectMapper) {
        this.diagnosticSessionRepository = diagnosticSessionRepository;
        this.answerRecordRepository = answerRecordRepository;
        this.redisUtil = redisUtil;
        this.diagnosticAgent = diagnosticAgent;
        this.knowledgeGraphService = knowledgeGraphService;
        this.masteryUpdateService = masteryUpdateService;
        this.questionBankService = questionBankService;
        this.studentProfileRepository = studentProfileRepository;
        this.llmService = llmService;
        this.objectMapper = objectMapper;
    }

    /**
     * Start a diagnostic session - AI generates adaptive questions
     */
    public Map<String, Object> startDiagnostic(Long studentId,
                                               String module,
                                               String difficulty,
                                               Map<String, Object> requestProfile) {
        // Check no in_progress session exists
        Optional<DiagnosticSession> existing = diagnosticSessionRepository.findByStudentIdAndStatus(studentId, "in_progress");
        if (existing.isPresent()) {
            Map<String, Object> conflictData = new LinkedHashMap<>();
            conflictData.put("diagnosticId", existing.get().getDiagnosticId());
            throw new BusinessException(409, "已有进行中的诊断会话，请先完成或结束当前诊断", conflictData);
        }

        // Generate diagnostic ID
        String diagnosticId = "diag_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "_" + UUID.randomUUID().toString().substring(0, 8);

        // Build AgentContext for question generation (profile first, so we can derive course)
        Map<String, Object> profileContext = buildStudentProfileContext(studentId, requestProfile);

        // Resolve effective course/module from explicit param or student profile
        String effectiveModule = module;
        if (effectiveModule == null || effectiveModule.isBlank()) {
            Object course = profileContext.get("currentCourse");
            if (course != null) {
                effectiveModule = String.valueOf(course);
            }
        }
        if (effectiveModule == null || effectiveModule.isBlank()) {
            throw new BusinessException(400, "诊断科目不能为空");
        }

        // Get knowledge points by course/major/education level
        String majorDirection = asText(profileContext.get("majorDirection"));
        String educationLevel = asText(profileContext.get("educationLevel"));
        // 任意科目判定：只看该科目本身是否在知识图谱里，不走专业方向兜底。
        // 否则学生明确要诊断「大学英语」时，会被画像里的 majorDirection（如计算机类）兜底成计算机题。
        boolean customSubject = !knowledgeGraphService.isKnownModule(effectiveModule);
        if (customSubject) {
            profileContext.put("currentCourse", effectiveModule);
            profileContext.put("diagnosticSubject", effectiveModule);
        }
        List<KnowledgeGraphService.KnowledgeNode> nodes = customSubject
                ? Collections.emptyList()
                : knowledgeGraphService.findNodesByCourse(majorDirection, effectiveModule, educationLevel);
        List<String> knowledgePointIds = nodes.stream()
                .map(KnowledgeGraphService.KnowledgeNode::getId)
                .collect(Collectors.toList());

        AgentContext context = AgentContext.builder()
                .studentId(studentId)
                .module(effectiveModule)
                .diagnosticId(diagnosticId)
                .studentProfile(profileContext)
                .build();
        context.putSessionData("action", "generate");
        context.putSessionData("knowledgePoints", knowledgePointIds);
        context.putSessionData("customSubject", customSubject);
        // 画像驱动：把基础水平与薄弱模块传给出题 Agent，用于难度基线与薄弱模块加权
        Object foundationLevel = profileContext.get("foundationLevel");
        if (foundationLevel != null) {
            context.putSessionData("foundationLevel", foundationLevel);
        }
        Object weakModules = profileContext.get("weakModulePriority");
        if (weakModules != null) {
            context.putSessionData("weakModulePriority", weakModules);
        }

        // Call DiagnosticAgent to generate questions via LLM
        // 刻意直调：诊断出题是事件链的源头（generate 分支不产出事件），无上游事件可经 orchestrator 触发。
        AgentResponse response = diagnosticAgent.execute(context);

        List<Map<String, Object>> questions = Collections.emptyList();
        String questionSource = "agent";
        if (response.isSuccess() && response.getData().containsKey("questions")) {
            questions = castQuestionList(response.getData().get("questions"));
        } else {
            log.warn("AI诊断题生成失败: {}", response.getMessage());
        }

        // 题库兜底按 kpId 检索，仅对预定义科目有意义；任意科目跳过，完全依赖 LLM
        if (questions.isEmpty() && !customSubject) {
            questions = questionBankService.findReusableDiagnosticQuestions(
                    knowledgePointIds, TOTAL_ESTIMATED_QUESTIONS);
            questionSource = "question_bank";
        }

        if (questions.isEmpty()) {
            String msg = customSubject
                    ? "暂时无法为「" + effectiveModule + "」生成诊断题目，请换个说法或稍后重试"
                    : "未能生成有效诊断题目，请稍后重试";
            throw new BusinessException(500, msg);
        }

        // Enrich questions with knowledge point names from graph（任意科目时 nodes 为空，沿用 AI 自带的 kpName）
        enrichQuestionsWithKnowledgeGraph(questions, nodes);
        int bankedQuestions = questionBankService.saveGeneratedQuestions(
                "diagnostic", effectiveModule, diagnosticId, questions);

        // Cache questions in Redis
        cacheQuestions(diagnosticId, questions);

        // Create session
        DiagnosticSession session = new DiagnosticSession();
        session.setDiagnosticId(diagnosticId);
        session.setStudentId(studentId);
        session.setModule(effectiveModule);
        session.setStatus("in_progress");
        session.setTotalQuestions(questions.size());
        session.setCurrentQuestionIndex(1);
        // 初始难度：显式参数优先，否则按基础水平自适应（基础→0.35 / 中等→0.50 / 较强→0.65）
        BigDecimal initialDifficulty = difficulty != null
                ? new BigDecimal(difficulty)
                : difficultyByFoundation(asText(profileContext.get("foundationLevel")));
        session.setCurrentDifficulty(initialDifficulty);
        session.setQuestionSnapshots(toJson(questions));
        diagnosticSessionRepository.save(session);

        // Cache session reference
        try {
            redisUtil.set(DIAG_CACHE_PREFIX + diagnosticId, session.getId(),
                    DIAG_CACHE_MINUTES, TimeUnit.MINUTES);
        } catch (Exception ignored) { }

        // Build first question for client (without correctAnswer)
        Map<String, Object> firstQuestion = buildClientQuestion(questions.get(0), 1);

        // Build response
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("diagnosticId", diagnosticId);
        result.put("module", effectiveModule);
        result.put("totalEstimatedQuestions", questions.size());
        result.put("currentQuestionIndex", 1);
        result.put("question", firstQuestion);
        result.put("snapshotSaved", true);
        result.put("questionBankSaved", bankedQuestions);
        result.put("questionSource", questionSource);
        result.put("fallbackUsed", "question_bank".equals(questionSource));
        return result;
    }

    /**
     * Submit an answer - check against AI-generated questions
     */
    public Map<String, Object> submitAnswer(Long studentId, String diagnosticId, Long questionId, String answer, Integer timeSpent) {
        DiagnosticSession session = diagnosticSessionRepository.findByDiagnosticId(diagnosticId)
                .orElseThrow(() -> new BusinessException(404, "诊断会话不存在"));

        if (!session.getStudentId().equals(studentId)) {
            throw new BusinessException(403, "无权操作此诊断会话");
        }
        if (!"in_progress".equals(session.getStatus())) {
            throw new BusinessException(400, "诊断会话已结束");
        }

        // Retrieve server-side question snapshots. Redis is only an acceleration layer;
        // persisted snapshots are the source of truth for grading.
        List<Map<String, Object>> questions = getQuestionSnapshots(session);
        if (questions == null || questions.isEmpty()) {
            throw new BusinessException(500, "诊断题目快照不存在，请重新开始诊断");
        }

        int currentIndex = session.getCurrentQuestionIndex();
        if (currentIndex > questions.size()) {
            throw new BusinessException(400, "所有题目已作答完毕");
        }

        Map<String, Object> currentQuestion = questions.get(currentIndex - 1);

        // 题型：choice/judge 客观题字符串判分；fill/subjective 主观类用 LLM 判分
        String qType = String.valueOf(currentQuestion.getOrDefault("type", "choice"));
        if (qType == null || qType.isBlank() || "null".equals(qType)) {
            qType = "choice";
        }
        boolean subjectiveType = "fill".equals(qType) || "subjective".equals(qType);

        // Get knowledge point info
        String kpId = String.valueOf(currentQuestion.getOrDefault("knowledgePointId", ""));
        String kpName = String.valueOf(currentQuestion.getOrDefault("knowledgePointName", ""));
        String errorType = String.valueOf(currentQuestion.getOrDefault("errorType", ""));

        boolean isCorrect;
        String correctAnswer;
        String aiFeedback = null;
        if (subjectiveType) {
            String referenceAnswer = String.valueOf(currentQuestion.getOrDefault("referenceAnswer",
                    currentQuestion.getOrDefault("correctAnswer", "")));
            correctAnswer = referenceAnswer;
            Map<String, Object> grade = gradeByLlm(
                    String.valueOf(currentQuestion.get("question")), referenceAnswer, answer, qType);
            isCorrect = Boolean.TRUE.equals(grade.get("correct"));
            aiFeedback = String.valueOf(grade.getOrDefault("feedback", ""));
            Object gErr = grade.get("errorType");
            if (gErr != null && !String.valueOf(gErr).isBlank()) {
                errorType = String.valueOf(gErr);
            }
        } else {
            correctAnswer = String.valueOf(currentQuestion.get("correctAnswer"));
            isCorrect = answer != null && answer.trim().equalsIgnoreCase(correctAnswer.trim());
        }

        // Create answer record
        AnswerRecord record = new AnswerRecord();
        record.setDiagnosticId(diagnosticId);
        record.setStudentId(studentId);
        record.setQuestionIndex(currentIndex);
        record.setQuestionId(questionId);
        record.setKnowledgePointId(kpId);
        record.setKnowledgePointName(kpName);
        record.setQuestionType(qType);
        record.setDifficulty(session.getCurrentDifficulty());
        record.setContent(String.valueOf(currentQuestion.get("question")));
        record.setStudentAnswer(answer);
        record.setCorrectAnswer(correctAnswer);
        record.setIsCorrect(isCorrect);
        record.setTimeSpent(timeSpent);

        // 主观类：AI 反馈写入 errorAnalysis（前端反馈区展示）；客观题错了给原有错误提示
        if (subjectiveType && aiFeedback != null && !aiFeedback.isBlank()) {
            record.setErrorAnalysis(aiFeedback);
            if (!isCorrect) {
                record.setErrorType(errorType);
            }
        } else if (!isCorrect) {
            record.setErrorType(errorType);
            record.setErrorAnalysis(generateErrorHint(errorType, kpName));
        }

        // Store options as JSON
        try {
            Object options = currentQuestion.get("options");
            if (options != null) {
                record.setOptions(objectMapper.writeValueAsString(options));
            }
        } catch (Exception ignored) { }

        answerRecordRepository.save(record);
        double updatedMastery = masteryUpdateService.updateFromAnswer(
                studentId, kpId, session.getModule(), isCorrect, "diagnostic");

        // Update session
        if (isCorrect) {
            session.setCorrectCount(session.getCorrectCount() + 1);
        }

        // Adaptive difficulty adjustment
        BigDecimal diffAdjust;
        if (isCorrect) {
            diffAdjust = session.getCurrentDifficulty().add(new BigDecimal("0.10"));
        } else {
            diffAdjust = session.getCurrentDifficulty().subtract(new BigDecimal("0.10"));
        }
        diffAdjust = diffAdjust.max(new BigDecimal("0.20")).min(new BigDecimal("0.95"));
        session.setCurrentDifficulty(diffAdjust);

        boolean isFinished = currentIndex >= questions.size();

        // Build result object
        Map<String, Object> resultObj = new LinkedHashMap<>();
        resultObj.put("questionId", questionId);
        resultObj.put("isCorrect", isCorrect);
        resultObj.put("correctAnswer", correctAnswer);
        resultObj.put("questionType", qType);
        resultObj.put("errorType", !isCorrect ? translateErrorType(errorType) : null);
        resultObj.put("errorDetail", !isCorrect ? errorType : null);
        // 主观类：始终展示 AI 反馈（无论对错）；客观题仅错时给错误分析
        if (subjectiveType) {
            resultObj.put("errorAnalysis", aiFeedback);
        } else {
            resultObj.put("errorAnalysis", !isCorrect ? record.getErrorAnalysis() : null);
        }
        resultObj.put("masteryAfter", BigDecimal.valueOf(updatedMastery).setScale(2, RoundingMode.HALF_UP));

        // Solution：客观题给正确答案/解析；主观类给参考答案
        String explanation;
        if (currentQuestion.get("explanation") != null) {
            explanation = String.valueOf(currentQuestion.get("explanation"));
        } else if (subjectiveType) {
            explanation = "参考答案：" + correctAnswer;
        } else {
            explanation = "正确答案：" + correctAnswer;
        }
        resultObj.put("solution", explanation);

        BigDecimal previousDifficulty = isCorrect
                ? diffAdjust.subtract(new BigDecimal("0.10"))
                : diffAdjust.add(new BigDecimal("0.10"));
        previousDifficulty = previousDifficulty.max(new BigDecimal("0.20")).min(new BigDecimal("0.95"));

        Map<String, Object> adaptiveInfo = new LinkedHashMap<>();
        adaptiveInfo.put("previousDifficulty", previousDifficulty);
        adaptiveInfo.put("nextDifficulty", diffAdjust);
        adaptiveInfo.put("adjustmentReason", isCorrect
                ? "答对当前题目，难度上调"
                : "答错当前题目，难度下调，深入诊断薄弱环节");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("diagnosticId", diagnosticId);
        result.put("result", resultObj);
        result.put("adaptiveInfo", adaptiveInfo);
        result.put("isFinished", isFinished);
        result.put("currentQuestionIndex", isFinished ? currentIndex : currentIndex + 1);
        result.put("totalEstimatedQuestions", questions.size());

        if (!isFinished) {
            session.setCurrentQuestionIndex(currentIndex + 1);
            Map<String, Object> nextQuestion = buildClientQuestion(questions.get(currentIndex), currentIndex + 1);
            result.put("nextQuestion", nextQuestion);
        } else {
            result.put("nextQuestion", null);
        }

        diagnosticSessionRepository.save(session);
        return result;
    }

    /**
     * Finish a diagnostic session - returns basic stats immediately, AI analysis runs async
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> finishDiagnostic(Long studentId, String diagnosticId) {
        DiagnosticSession session = diagnosticSessionRepository.findByDiagnosticId(diagnosticId)
                .orElseThrow(() -> new BusinessException(404, "诊断会话不存在"));

        if (!session.getStudentId().equals(studentId)) {
            throw new BusinessException(403, "无权操作此诊断会话");
        }
        if ("completed".equals(session.getStatus())) {
            throw new BusinessException(400, "诊断会话已完成");
        }

        List<AnswerRecord> records = answerRecordRepository.findByDiagnosticIdOrderByQuestionIndexAsc(diagnosticId);
        int totalAnswered = records.size();
        int correctCount = (int) records.stream().filter(r -> Boolean.TRUE.equals(r.getIsCorrect())).count();

        BigDecimal accuracy = totalAnswered > 0
                ? new BigDecimal(correctCount).divide(new BigDecimal(totalAnswered), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Build knowledge point results from actual records (fast, no AI)
        List<Map<String, Object>> knowledgePointResults = buildKnowledgePointResults(records);
        for (Map<String, Object> kpResult : knowledgePointResults) {
            Object kpIdObj = kpResult.get("knowledgePointId");
            Object masteryObj = kpResult.get("mastery");
            if (kpIdObj != null && masteryObj instanceof Number) {
                masteryUpdateService.recordKnowledgePointMastery(
                        studentId,
                        String.valueOf(kpIdObj),
                        session.getModule(),
                        ((Number) masteryObj).doubleValue(),
                        "diagnostic_summary");
            }
        }

        // Compute local fallback weak points & error patterns immediately
        List<Map<String, Object>> weakPoints = buildWeakPointsFallback(records);
        Map<String, Integer> errorPatterns = buildErrorPatternsFallback(records);
        String suggestion = buildSuggestionFallback(session.getModule(), accuracy, weakPoints);

        // Update session with basic results first (fast response)
        session.setStatus("completed");
        session.setTotalQuestions(totalAnswered);
        session.setCorrectCount(correctCount);
        session.setAccuracy(accuracy);
        session.setOverallMastery(accuracy.multiply(new BigDecimal("0.85")).add(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP));
        session.setKnowledgePointResults(toJson(knowledgePointResults));
        session.setWeakPoints(toJson(weakPoints));
        session.setErrorPatterns(toJson(errorPatterns));
        session.setSuggestion(suggestion);
        session.setEndTime(LocalDateTime.now());
        diagnosticSessionRepository.save(session);

        // 随学随新：把诊断算出的易错点/薄弱模块回写学生画像（激活 errorPatterns 维度）
        writeBackDiagnosticToProfile(studentId, errorPatterns, weakPoints);

        // Retrieve persisted questions for async AI analysis.
        List<Map<String, Object>> questions = getQuestionSnapshots(session);

        // Launch AI analysis asynchronously - will update session in background
        if (questions != null && !questions.isEmpty()) {
            asyncExecutor.submit(() -> runAsyncAIAnalysis(
                    studentId, diagnosticId, session.getModule(), questions, records, accuracy));
        }

        // Clear Redis cache
        try {
            redisUtil.delete(DIAG_CACHE_PREFIX + diagnosticId);
        } catch (Exception ignored) { }

        // Build response immediately with local analysis
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("diagnosticId", diagnosticId);
        result.put("module", session.getModule());
        result.put("startTime", session.getStartTime());
        result.put("endTime", session.getEndTime());
        result.put("totalQuestions", totalAnswered);
        result.put("correctCount", correctCount);
        result.put("incorrectCount", totalAnswered - correctCount);
        result.put("accuracy", accuracy);
        result.put("overallMastery", session.getOverallMastery());
        result.put("knowledgePointResults", knowledgePointResults);
        result.put("weakPoints", weakPoints);
        result.put("errorPatterns", errorPatterns);
        result.put("suggestion", suggestion);
        result.put("canTracing", !weakPoints.isEmpty());
        result.put("aiAnalysisPending", questions != null && !questions.isEmpty());
        return result;
    }

    /**
     * Async AI analysis - updates session with richer results in background
     */
    @SuppressWarnings("unchecked")
    private void runAsyncAIAnalysis(Long studentId, String diagnosticId, String module,
                                     List<Map<String, Object>> questions, List<AnswerRecord> records,
                                     BigDecimal accuracy) {
        try {
            log.info("开始异步AI分析: diagnosticId={}", diagnosticId);

            Map<String, String> answersMap = new LinkedHashMap<>();
            for (AnswerRecord r : records) {
                String qId = "q" + r.getQuestionIndex();
                answersMap.put(qId, r.getStudentAnswer() != null ? r.getStudentAnswer() : "");
            }

            AgentContext context = AgentContext.builder()
                    .studentId(studentId)
                    .module(module)
                    .diagnosticId(diagnosticId)
                    .build();
            context.putSessionData("action", "analyze");
            context.putSessionData("questions", questions);
            context.putSessionData("answers", answersMap);

            // 刻意直调：诊断分析（DiagnosticAgent）是事件链源头，自身产出 DIAGNOSIS_COMPLETE 供溯源端点消费，
            // 但它不消费任何上游事件，故无需经 orchestrator 触发。溯源端点会在后续独立请求里 fire DIAGNOSIS_COMPLETE。
            AgentResponse analyzeResponse = diagnosticAgent.execute(context);

            if (analyzeResponse.isSuccess()) {
                Map<String, Object> analysisData = analyzeResponse.getData();

                List<Map<String, Object>> weakPoints;
                Object wpObj = analysisData.get("weakPoints");
                weakPoints = wpObj instanceof List ? (List<Map<String, Object>>) wpObj : buildWeakPointsFallback(records);

                Map<String, Integer> errorPatterns;
                Object epObj = analysisData.get("errorPatterns");
                errorPatterns = epObj instanceof Map ? (Map<String, Integer>) epObj : buildErrorPatternsFallback(records);

                String overallAssessment = analysisData.get("overallAssessment") != null
                        ? String.valueOf(analysisData.get("overallAssessment")) : null;
                String suggestion = buildSuggestionFromAI(module, accuracy, weakPoints, overallAssessment);

                // Update session with AI analysis
                DiagnosticSession session = diagnosticSessionRepository.findByDiagnosticId(diagnosticId).orElse(null);
                if (session != null) {
                    session.setWeakPoints(toJson(weakPoints));
                    session.setErrorPatterns(toJson(errorPatterns));
                    session.setSuggestion(suggestion);
                    diagnosticSessionRepository.save(session);
                    log.info("异步AI分析完成: diagnosticId={}", diagnosticId);
                }
            } else {
                log.warn("异步AI分析失败，保留本地分析: {}", analyzeResponse.getMessage());
            }
        } catch (Exception e) {
            log.error("异步AI分析异常: diagnosticId={}, error={}", diagnosticId, e.getMessage());
        } finally {
            // Clean up questions cache after analysis
            try {
                redisUtil.delete(DIAG_QUESTIONS_PREFIX + diagnosticId);
            } catch (Exception ignored) { }
        }
    }

    /**
     * Get paginated history
     */
    public Map<String, Object> getHistory(Long studentId, String module, int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(page, pageSize);
        Page<DiagnosticSession> pageResult;
        if (module != null && !module.isEmpty()) {
            pageResult = diagnosticSessionRepository.findByStudentIdAndModuleOrderByStartTimeDesc(studentId, module, pageRequest);
        } else {
            pageResult = diagnosticSessionRepository.findByStudentIdOrderByStartTimeDesc(studentId, pageRequest);
        }

        List<Map<String, Object>> records = new ArrayList<>();
        for (DiagnosticSession s : pageResult.getContent()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("diagnosticId", s.getDiagnosticId());
            item.put("module", s.getModule());
            item.put("status", s.getStatus());
            item.put("totalQuestions", s.getTotalQuestions());
            item.put("correctCount", s.getCorrectCount());
            item.put("accuracy", s.getAccuracy());
            item.put("overallMastery", s.getOverallMastery());
            item.put("startTime", s.getStartTime());
            item.put("endTime", s.getEndTime());
            records.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", pageResult.getTotalElements());
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", pageResult.getTotalPages());
        return result;
    }

    /**
     * Get full diagnostic result
     */
    public Map<String, Object> getDiagnosticResult(Long studentId, String diagnosticId) {
        DiagnosticSession session = diagnosticSessionRepository.findByDiagnosticId(diagnosticId)
                .orElseThrow(() -> new BusinessException(404, "诊断会话不存在"));

        if (!session.getStudentId().equals(studentId)) {
            throw new BusinessException(403, "无权查看此诊断结果");
        }

        List<AnswerRecord> records = answerRecordRepository.findByDiagnosticIdOrderByQuestionIndexAsc(diagnosticId);

        List<Map<String, Object>> answerRecords = new ArrayList<>();
        for (AnswerRecord r : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("questionIndex", r.getQuestionIndex());
            item.put("questionId", r.getQuestionId());
            item.put("knowledgePointId", r.getKnowledgePointId());
            item.put("knowledgePointName", r.getKnowledgePointName());
            item.put("questionType", r.getQuestionType());
            item.put("difficulty", r.getDifficulty());
            item.put("content", r.getContent());
            item.put("studentAnswer", r.getStudentAnswer());
            item.put("correctAnswer", r.getCorrectAnswer());
            item.put("isCorrect", r.getIsCorrect());
            item.put("timeSpent", r.getTimeSpent());
            item.put("errorType", r.getErrorType());
            item.put("errorAnalysis", r.getErrorAnalysis());
            answerRecords.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("diagnosticId", diagnosticId);
        result.put("module", session.getModule());
        result.put("status", session.getStatus());
        result.put("totalQuestions", session.getTotalQuestions());
        result.put("correctCount", session.getCorrectCount());
        result.put("accuracy", session.getAccuracy());
        result.put("overallMastery", session.getOverallMastery());
        result.put("answerRecords", answerRecords);
        result.put("knowledgePointResults", parseJson(session.getKnowledgePointResults()));
        result.put("weakPoints", parseJson(session.getWeakPoints()));
        result.put("errorPatterns", parseJson(session.getErrorPatterns()));
        result.put("suggestion", session.getSuggestion());
        result.put("startTime", session.getStartTime());
        result.put("endTime", session.getEndTime());

        // Check if async AI analysis is still pending (questions cache still exists)
        boolean aiPending = false;
        try {
            Object cached = redisUtil.get(DIAG_QUESTIONS_PREFIX + diagnosticId);
            aiPending = cached != null;
        } catch (Exception ignored) { }
        result.put("aiAnalysisPending", aiPending);
        result.put("questionSnapshotSaved", session.getQuestionSnapshots() != null && !session.getQuestionSnapshots().isBlank());

        return result;
    }

    /**
     * Generate a question (public API preserved for compatibility)
     */
    public Map<String, Object> generateQuestion(String module, BigDecimal difficulty, int questionIndex) {
        // This method is kept for API compatibility but now returns a placeholder
        // Real questions are generated via AI in startDiagnostic
        Map<String, Object> question = new LinkedHashMap<>();
        question.put("questionId", (long) (questionIndex * 1000 + 1));
        question.put("questionIndex", questionIndex);
        question.put("module", module);
        question.put("difficulty", difficulty);
        question.put("content", "题目加载中...");
        question.put("type", "choice");
        return question;
    }

    // ============ Internal Helpers ============

    private Map<String, Object> buildStudentProfileContext(Long studentId, Map<String, Object> requestProfile) {
        Map<String, Object> context = new LinkedHashMap<>();
        StudentProfile profile = studentProfileRepository.findByStudentId(studentId).orElse(null);
        if (profile != null) {
            putIfPresent(context, "majorDirection", profile.getMajorDirection());
            putIfPresent(context, "educationLevel", profile.getEducationLevel());
            putIfPresent(context, "currentCourse", profile.getCurrentCourse());
            putIfPresent(context, "learningGoal", profile.getLearningGoal());
            putIfPresent(context, "foundationLevel", profile.getFoundationLevel());
            putIfPresent(context, "resourcePreference", profile.getResourcePreference());
            putIfPresent(context, "academicInterest", profile.getAcademicInterest());
            putIfPresent(context, "learningStyle", profile.getLearningStyle());
            putIfPresent(context, "dailyStudyMinutes", profile.getDailyStudyMinutes());
            putIfPresent(context, "weakModulePriority", profile.getWeakModulePriority());
            putIfPresent(context, "studyMode", profile.getStudyMode());
            putIfPresent(context, "overallMastery", profile.getOverallMastery());
            putIfPresent(context, "abilityParam", profile.getAbilityParam());
        }
        if (requestProfile != null) {
            copyProfileValue(context, requestProfile, "majorDirection");
            copyProfileValue(context, requestProfile, "educationLevel");
            copyProfileValue(context, requestProfile, "currentCourse");
            copyProfileValue(context, requestProfile, "course");
            copyProfileValue(context, requestProfile, "learningGoal");
            copyProfileValue(context, requestProfile, "foundationLevel");
            copyProfileValue(context, requestProfile, "resourcePreference");
            copyProfileValue(context, requestProfile, "academicInterest");
        }
        if (context.containsKey("course") && !context.containsKey("currentCourse")) {
            context.put("currentCourse", context.get("course"));
        }
        return context;
    }

    private void copyProfileValue(Map<String, Object> target, Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value instanceof String && ((String) value).isBlank()) return;
        if (value != null) target.put(key, value);
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value == null) return;
        if (value instanceof String && ((String) value).isBlank()) return;
        target.put(key, value);
    }

    private String asText(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * Build a client-facing question (without correctAnswer)
     */
    private Map<String, Object> buildClientQuestion(Map<String, Object> aiQuestion, int index) {
        Map<String, Object> clientQuestion = new LinkedHashMap<>();
        clientQuestion.put("questionId", (long) (index * 1000 + 1));
        clientQuestion.put("questionIndex", index);
        clientQuestion.put("knowledgePointId", aiQuestion.get("knowledgePointId"));
        clientQuestion.put("knowledgePointName", aiQuestion.get("knowledgePointName"));
        // 透传 AI 题型（choice/judge/fill/subjective），缺省回退 choice
        String type = String.valueOf(aiQuestion.getOrDefault("type", "choice"));
        if (type == null || type.isBlank() || "null".equals(type)) {
            type = "choice";
        }
        clientQuestion.put("type", type);
        clientQuestion.put("content", aiQuestion.get("question"));

        // 仅当题目带 options（单选/判断）时转换并下发；填空/主观无 options，前端走输入框分支
        Object optionsObj = aiQuestion.get("options");
        if (optionsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> optionsMap = (Map<String, String>) optionsObj;
            List<Map<String, String>> optionsList = new ArrayList<>();
            for (Map.Entry<String, String> entry : optionsMap.entrySet()) {
                Map<String, String> opt = new LinkedHashMap<>();
                opt.put("label", entry.getKey());
                opt.put("text", String.valueOf(entry.getValue()));
                optionsList.add(opt);
            }
            clientQuestion.put("options", optionsList);
        }

        // Difficulty from AI (1-5) → normalized 0-1
        Object diffObj = aiQuestion.get("difficulty");
        if (diffObj instanceof Number) {
            double diffVal = ((Number) diffObj).doubleValue();
            clientQuestion.put("difficulty", new BigDecimal(diffVal / 5.0).setScale(2, RoundingMode.HALF_UP));
        }

        // Time limit based on difficulty
        int timeLimit = 120;
        if (diffObj instanceof Number) {
            int diff = ((Number) diffObj).intValue();
            if (diff >= 4) timeLimit = 300;
            else if (diff >= 3) timeLimit = 180;
        }
        clientQuestion.put("timeLimit", timeLimit);

        return clientQuestion;
    }

    /**
     * Enrich AI questions with knowledge point names from knowledge graph
     */
    private void enrichQuestionsWithKnowledgeGraph(List<Map<String, Object>> questions,
                                                    List<KnowledgeGraphService.KnowledgeNode> nodes) {
        Map<String, String> idToName = nodes.stream()
                .collect(Collectors.toMap(
                        KnowledgeGraphService.KnowledgeNode::getId,
                        KnowledgeGraphService.KnowledgeNode::getName,
                        (a, b) -> a));

        for (int i = 0; i < questions.size(); i++) {
            Map<String, Object> q = questions.get(i);
            // Ensure id follows q1, q2... pattern
            q.put("id", "q" + (i + 1));

            String kpId = String.valueOf(q.getOrDefault("knowledgePointId", ""));
            if (idToName.containsKey(kpId)) {
                q.put("knowledgePointName", idToName.get(kpId));
            } else {
                // AI might have generated a different ID format, try to match
                q.putIfAbsent("knowledgePointName", kpId);
            }
        }
    }

    /**
     * Cache questions in Redis
     */
    private void cacheQuestions(String diagnosticId, List<Map<String, Object>> questions) {
        try {
            String json = objectMapper.writeValueAsString(questions);
            redisUtil.set(DIAG_QUESTIONS_PREFIX + diagnosticId, json, DIAG_CACHE_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存诊断题目失败: {}", e.getMessage());
        }
    }

    /**
     * Retrieve cached questions from Redis
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getCachedQuestions(String diagnosticId) {
        try {
            Object cached = redisUtil.get(DIAG_QUESTIONS_PREFIX + diagnosticId);
            if (cached == null) return null;
            String json = cached.toString();
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("读取缓存诊断题目失败: {}", e.getMessage());
            return null;
        }
    }

    private List<Map<String, Object>> getQuestionSnapshots(DiagnosticSession session) {
        List<Map<String, Object>> cachedQuestions = getCachedQuestions(session.getDiagnosticId());
        if (cachedQuestions != null && !cachedQuestions.isEmpty()) {
            return cachedQuestions;
        }
        if (session.getQuestionSnapshots() == null || session.getQuestionSnapshots().isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(session.getQuestionSnapshots(),
                    new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("读取诊断题目快照失败: diagnosticId={}, error={}",
                    session.getDiagnosticId(), e.getMessage());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castQuestionList(Object obj) {
        if (obj instanceof List) {
            return (List<Map<String, Object>>) obj;
        }
        return Collections.emptyList();
    }

    private String generateErrorHint(String errorType, String knowledgePointName) {
        if (errorType == null || errorType.isEmpty()) {
            return "在「" + knowledgePointName + "」方面存在不足，建议重点复习。";
        }
        return "在「" + knowledgePointName + "」方面存在「" + translateErrorType(errorType) + "」，建议针对性练习。";
    }

    private String translateErrorType(String errorType) {
        if (errorType == null || errorType.isEmpty()) return "其他错误";
        switch (errorType) {
            case "concept_confusion": return "概念性错误";
            case "calculation_error": return "计算性错误";
            case "method_misuse":
            case "rule_misapplication": return "方法性错误";
            case "knowledge_gap": return "知识缺漏";
            case "careless_mistake":
            case "omission_error": return "审题性错误";
            case "formula_error": return "公式记忆错误";
            case "sign_error": return "符号运算错误";
            case "chain_rule_error": return "链式法则错误";
            case "logic_error": return "逻辑推理错误";
            case "case_analysis_error": return "分类讨论不完整";
            default: return errorType;
        }
    }

    // ============ Fallback methods (used when AI analysis fails) ============

    private List<Map<String, Object>> buildWeakPointsFallback(List<AnswerRecord> records) {
        Map<String, List<AnswerRecord>> grouped = new LinkedHashMap<>();
        for (AnswerRecord r : records) {
            if (r.getKnowledgePointId() != null) {
                grouped.computeIfAbsent(r.getKnowledgePointId(), k -> new ArrayList<>()).add(r);
            }
        }

        List<Map<String, Object>> weakPoints = new ArrayList<>();
        for (Map.Entry<String, List<AnswerRecord>> entry : grouped.entrySet()) {
            List<AnswerRecord> group = entry.getValue();
            int total = group.size();
            int correct = (int) group.stream().filter(r -> Boolean.TRUE.equals(r.getIsCorrect())).count();
            double mastery = (double) correct / total;

            if (mastery < 0.6) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("kpId", entry.getKey());
                item.put("kpName", group.get(0).getKnowledgePointName());
                item.put("masteryLevel", BigDecimal.valueOf(mastery).setScale(2, RoundingMode.HALF_UP));
                weakPoints.add(item);
            }
        }
        weakPoints.sort((a, b) -> {
            double ma = ((Number) a.get("masteryLevel")).doubleValue();
            double mb = ((Number) b.get("masteryLevel")).doubleValue();
            return Double.compare(ma, mb);
        });
        return weakPoints;
    }

    private Map<String, Integer> buildErrorPatternsFallback(List<AnswerRecord> records) {
        Map<String, Integer> patterns = new LinkedHashMap<>();
        for (AnswerRecord r : records) {
            if (r.getErrorType() != null && !r.getErrorType().isEmpty()) {
                String chineseType = translateErrorType(r.getErrorType());
                patterns.merge(chineseType, 1, Integer::sum);
            }
        }
        return patterns;
    }

    private String buildSuggestionFallback(String module, BigDecimal accuracy, List<Map<String, Object>> weakPoints) {
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(module).append("模块诊断总结】\n");
        sb.append("整体正确率：").append(accuracy.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP)).append("%。\n\n");

        if (accuracy.compareTo(new BigDecimal("0.8")) >= 0) {
            sb.append("总体掌握情况良好，基础扎实。");
        } else if (accuracy.compareTo(new BigDecimal("0.6")) >= 0) {
            sb.append("基础知识基本掌握，但部分知识点需要加强。");
        } else {
            sb.append("基础知识存在较多薄弱环节，建议系统复习。");
        }

        if (!weakPoints.isEmpty()) {
            sb.append("\n\n薄弱知识点：\n");
            for (Map<String, Object> wp : weakPoints) {
                sb.append("- ").append(wp.get("kpName")).append("\n");
            }
        }
        sb.append("\n建议学习路径：先巩固基础概念，再进行专项练习，最后通过综合题目检验学习效果。");
        return sb.toString();
    }

    private String buildSuggestionFromAI(String module, BigDecimal accuracy,
                                          List<Map<String, Object>> weakPoints, String overallAssessment) {
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(module).append("模块诊断总结】\n");
        sb.append("整体正确率：").append(accuracy.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP)).append("%。\n\n");

        if (overallAssessment != null && !overallAssessment.isEmpty()) {
            sb.append(overallAssessment).append("\n");
        }

        if (!weakPoints.isEmpty()) {
            sb.append("\n薄弱知识点：\n");
            for (Map<String, Object> wp : weakPoints) {
                sb.append("- ").append(wp.get("kpName"));
                Object ml = wp.get("masteryLevel");
                if (ml instanceof Number) {
                    sb.append("（掌握度：").append(String.format("%.0f%%", ((Number) ml).doubleValue() * 100)).append("）");
                }
                sb.append("\n");
            }
        }
        sb.append("\n建议：先巩固薄弱知识点的基础概念，再进行针对性练习提升。");
        return sb.toString();
    }

    private List<Map<String, Object>> buildKnowledgePointResults(List<AnswerRecord> records) {
        Map<String, List<AnswerRecord>> grouped = new LinkedHashMap<>();
        for (AnswerRecord r : records) {
            if (r.getKnowledgePointId() != null) {
                grouped.computeIfAbsent(r.getKnowledgePointId(), k -> new ArrayList<>()).add(r);
            }
        }

        List<Map<String, Object>> results = new ArrayList<>();
        for (Map.Entry<String, List<AnswerRecord>> entry : grouped.entrySet()) {
            List<AnswerRecord> group = entry.getValue();
            int total = group.size();
            int correct = (int) group.stream().filter(r -> Boolean.TRUE.equals(r.getIsCorrect())).count();
            BigDecimal mastery = new BigDecimal(correct).divide(new BigDecimal(total), 2, RoundingMode.HALF_UP);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("knowledgePointId", entry.getKey());
            item.put("knowledgePointName", group.get(0).getKnowledgePointName());
            item.put("totalQuestions", total);
            item.put("correctCount", correct);
            item.put("mastery", mastery);
            String status;
            if (mastery.compareTo(new BigDecimal("0.9")) >= 0) {
                status = "熟练";
            } else if (mastery.compareTo(new BigDecimal("0.7")) >= 0) {
                status = "基本掌握";
            } else if (mastery.compareTo(new BigDecimal("0.5")) >= 0) {
                status = "一般";
            } else {
                status = "薄弱";
            }
            item.put("status", status);
            results.add(item);
        }
        return results;
    }

    /**
     * 按基础水平映射诊断初始难度：基础弱先验基础、基础强直接进阶。
     * 兼容多种表述（基础/较弱/入门 → 0.35；较强/扎实/进阶 → 0.65；其余中等 0.50）。
     */
    private BigDecimal difficultyByFoundation(String foundationLevel) {
        if (foundationLevel == null || foundationLevel.isBlank()) {
            return new BigDecimal("0.50");
        }
        String f = foundationLevel.trim();
        if (f.contains("较强") || f.contains("扎实") || f.contains("进阶") || f.contains("强")) {
            return new BigDecimal("0.65");
        }
        if (f.contains("中") ) {
            return new BigDecimal("0.50");
        }
        if (f.contains("基础") || f.contains("弱") || f.contains("入门") || f.contains("差")) {
            return new BigDecimal("0.35");
        }
        return new BigDecimal("0.50");
    }

    /**
     * 用 LLM 判分填空/主观题：返回 {correct, score, feedback, errorType}。
     * 失败兜底：填空退化为与参考答案字符串比较；主观题不打断（按未错处理）。
     */
    private Map<String, Object> gradeByLlm(String question, String referenceAnswer,
                                           String studentAnswer, String qType) {
        Map<String, Object> result = new HashMap<>();
        String stuAns = studentAnswer == null ? "" : studentAnswer.trim();
        try {
            boolean isFill = "fill".equals(qType);
            String sys = "你是严格而公正的智能阅卷助手。根据题目、参考答案和学生作答判断对错并给出简短反馈，"
                    + "只返回严格 JSON：{\"correct\":true/false,\"score\":0~1的小数,\"feedback\":\"一句话反馈\",\"errorType\":\"错误类型(对则空串)\"}。"
                    + (isFill
                        ? "这是填空题：学生答案与参考答案语义一致即算对（同义、近义、大小写或措辞差异都应判对），只有实质性错误才判错。"
                        : "这是主观/简答题：按要点覆盖程度判分，覆盖主要要点即算对(correct=true)，可在 feedback 中指出可改进处。")
                    + "feedback 用中文，不超过60字。";
            String user = "【题目】" + question + "\n【参考答案/评分要点】" + referenceAnswer
                    + "\n【学生作答】" + (stuAns.isBlank() ? "（未作答）" : stuAns);
            String resp = llmService.chatJsonSync(user, sys, 0.2);
            Map<String, Object> data = objectMapper.readValue(resp,
                    new TypeReference<Map<String, Object>>() {});
            result.put("correct", Boolean.TRUE.equals(data.get("correct")));
            result.put("score", data.get("score"));
            result.put("feedback", data.getOrDefault("feedback", ""));
            result.put("errorType", data.getOrDefault("errorType", ""));
            return result;
        } catch (Exception e) {
            log.warn("AI 判分失败，使用兜底: type={}, err={}", qType, e.getMessage());
            if ("fill".equals(qType)) {
                boolean eq = !stuAns.isBlank() && referenceAnswer != null
                        && stuAns.equalsIgnoreCase(referenceAnswer.trim());
                result.put("correct", eq);
                result.put("feedback", eq ? "回答正确" : "参考答案：" + referenceAnswer);
            } else {
                // 主观题无法判分时不打断，按未错处理并提示待复核
                result.put("correct", true);
                result.put("feedback", "已记录你的作答，AI 判分暂不可用，可对照参考答案自评：" + referenceAnswer);
            }
            result.put("errorType", "");
            return result;
        }
    }

    // ============ JSON Helpers ============
    /**
     * 把诊断算出的易错点模式与薄弱知识点回写学生画像，使「易错点」维度随学更新。
     * errorPatterns 直接写入此前的死字段 StudentProfile.errorPatterns；
     * weakPoints 的知识点名合并进 weakModulePriority。失败只记 log，不影响诊断主流程。
     */
    private void writeBackDiagnosticToProfile(Long studentId,
                                              Map<String, Integer> errorPatterns,
                                              List<Map<String, Object>> weakPoints) {
        try {
            StudentProfile profile = studentProfileRepository.findByStudentId(studentId).orElse(null);
            if (profile == null) {
                return;
            }
            boolean changed = false;

            if (errorPatterns != null && !errorPatterns.isEmpty()) {
                profile.setErrorPatterns(toJson(errorPatterns));
                changed = true;
            }

            if (weakPoints != null && !weakPoints.isEmpty()) {
                List<String> weakNames = new ArrayList<>();
                for (Map<String, Object> wp : weakPoints) {
                    Object name = wp.get("kpName");
                    if (name != null && !String.valueOf(name).isBlank()) {
                        String n = String.valueOf(name);
                        if (!weakNames.contains(n)) {
                            weakNames.add(n);
                        }
                    }
                }
                if (!weakNames.isEmpty()) {
                    profile.setWeakModulePriority(toJson(weakNames));
                    changed = true;
                }
            }

            if (changed) {
                studentProfileRepository.save(profile);
                // 失效画像总览缓存（与 ProfileService 同一 key 前缀）
                try {
                    redisUtil.delete("profile:overview:" + studentId);
                } catch (Exception ignored) { }
            }
        } catch (Exception e) {
            log.warn("诊断结果回写画像失败（忽略）: studentId={}, err={}", studentId, e.getMessage());
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    private Object parseJson(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }
}
