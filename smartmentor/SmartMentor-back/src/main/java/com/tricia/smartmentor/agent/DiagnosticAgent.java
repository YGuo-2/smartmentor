package com.tricia.smartmentor.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tricia.smartmentor.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * AI 驱动的学习诊断 Agent。
 * <p>
 * 支持两种动作（通过 {@code context.sessionData.get("action")} 区分）：
 * <ul>
 *   <li><b>generate</b> — 根据模块和学生画像，生成 8 道自适应诊断题（IRT 思想）</li>
 *   <li><b>analyze</b>  — 分析学生答题结果，输出薄弱点、错误模式和详细分析</li>
 * </ul>
 */
@Slf4j
@Component
public class DiagnosticAgent extends BaseAgent {

    private static final int TARGET_QUESTION_COUNT = 8;
    private static final int MIN_QUESTION_COUNT = 6;

    public DiagnosticAgent(LlmService llmService, ObjectMapper objectMapper) {
        super(llmService, objectMapper);
    }

    // ------------------------------------------------------------------ 身份与配置

    @Override
    protected String getName() {
        return "诊断Agent";
    }

    @Override
    protected String getSystemPrompt() {
        String fallback = "你是SmartMentor智学导师系统的高校课程诊断Agent，专门负责多专业课程的个性化学习诊断。" +
                "你的职责包括：根据学生的专业方向、学历层次、当前课程、学习目标和学习历史生成自适应诊断题目，以及分析学生答题结果识别薄弱知识点和错误模式。" +
                "请严格按照要求的JSON格式输出结果，不要附加任何额外文字。";
        return loadPromptTemplate(getPromptTemplateKey(), "diagnostic-system-v1", fallback).getContent();
    }

    @Override
    protected String getPromptTemplateKey() {
        return "diagnostic-system";
    }

    /**
     * 题目生成需要多样性，使用较高温度 0.7；分析则需要精确性，使用 0.3。
     * 此处返回生成阶段的默认值；分析时在 callLLM 中动态调整。
     */
    @Override
    protected double getTemperature() {
        return 0.7;
    }

    /**
     * 覆盖 callLLM：根据 action 类型动态调整温度。
     * <ul>
     *   <li>generate：0.7（需要题目多样性）</li>
     *   <li>analyze：0.3（需要分析精确性）</li>
     * </ul>
     */
    @Override
    protected String callLLM(String prompt, AgentContext context) {
        String action = resolveAction(context);
        double temperature = "analyze".equals(action) ? 0.3 : 0.7;
        // 使用 JSON mode 加速结构化输出
        return llmService.chatJsonSync(prompt, getSystemPrompt(), temperature);
    }

    // ------------------------------------------------------------------ 核心流程

    @Override
    protected String buildPrompt(AgentContext context) {
        String action = resolveAction(context);
        if ("analyze".equals(action)) {
            return buildAnalyzePrompt(context);
        }
        return buildGeneratePrompt(context);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, Object> parseResponse(String llmResponse, AgentContext context) {
        String action = resolveAction(context);
        if ("analyze".equals(action)) {
            return parseAnalyzeResponse(llmResponse, context);
        }
        return parseGenerateResponse(llmResponse, context);
    }

    @Override
    protected AgentResponse qualityCheck(Map<String, Object> parsed, AgentContext context) {
        String action = resolveAction(context);
        if ("analyze".equals(action)) {
            return qualityCheckAnalyze(parsed, context);
        }
        return qualityCheckGenerate(parsed, context);
    }

    // ================================================================== generate 逻辑

    /**
     * 构建题目生成 prompt，融合 IRT 自适应思想。
     */
    @SuppressWarnings("unchecked")
    private String buildGeneratePrompt(AgentContext context) {
        String module = context.getModule();
        Map<String, Object> sessionData = context.getSessionData();
        Map<String, Object> profile = context.getStudentProfile();
        Map<String, Double> mastery = context.getKnowledgeMastery();

        // 从 sessionData 获取本模块的知识点列表
        List<String> knowledgePoints = Collections.emptyList();
        Object kpObj = sessionData.get("knowledgePoints");
        if (kpObj instanceof List) {
            knowledgePoints = (List<String>) kpObj;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## 任务\n");
        sb.append("请为以下高校课程模块生成").append(TARGET_QUESTION_COUNT).append("道自适应诊断题目。\n\n");

        sb.append("## 课程模块信息\n");
        sb.append("- 模块名称：").append(module).append("\n");
        if (!knowledgePoints.isEmpty()) {
            sb.append("- 涉及知识点：").append(String.join("、", knowledgePoints)).append("\n");
        }
        sb.append("\n");

        // 任意科目：知识图谱中无预定义知识点时，让 LLM 作为该学科专家自行规划知识点
        boolean customSubject = Boolean.TRUE.equals(sessionData.get("customSubject")) || knowledgePoints.isEmpty();
        if (customSubject) {
            sb.append("## 任意科目出题要求\n");
            sb.append("本次诊断科目只允许是「").append(module).append("」。请你作为「").append(module).append("」的资深学习诊断专家，")
              .append("自行梳理该科目的核心知识点（覆盖主要主题、循序渐进），据此生成诊断题。\n");
            sb.append("硬性约束：所有题干、选项、参考答案、knowledgePointName 必须属于「").append(module)
              .append("」；不得生成计算机、Java Web、人工智能、数字电路或任何其它课程的题。\n");
            sb.append("学生画像中的专业方向、兴趣方向只用于调节难度和例句背景，不得覆盖本次诊断科目。\n");
            sb.append("每道题必须填写贴切的中文知识点名称到 knowledgePointName 字段（用于后续薄弱点分析），")
              .append("knowledgePointId 可用该科目英文/拼音缩写_序号（如 eng_01），不要使用其它科目的知识点。\n\n");
        }

        // IRT 自适应：利用学生历史数据调节难度分布
        if (profile != null && !profile.isEmpty()) {
            sb.append("## 学生画像\n");
            if (profile.containsKey("majorDirection")) sb.append("- 专业方向：").append(profile.get("majorDirection")).append("\n");
            if (profile.containsKey("educationLevel")) sb.append("- 学历层次：").append(profile.get("educationLevel")).append("\n");
            if (profile.containsKey("currentCourse")) sb.append("- 当前课程：").append(profile.get("currentCourse")).append("\n");
            if (profile.containsKey("learningGoal")) sb.append("- 学习目标：").append(profile.get("learningGoal")).append("\n");
            if (profile.containsKey("foundationLevel")) sb.append("- 基础水平：").append(profile.get("foundationLevel")).append("\n");
            if (profile.containsKey("resourcePreference")) sb.append("- 资源偏好：").append(profile.get("resourcePreference")).append("\n");
            if (profile.containsKey("academicInterest")) sb.append("- 兴趣方向：").append(profile.get("academicInterest")).append("\n");
            if (profile.containsKey("learningStyle")) {
                sb.append("- 学习风格：").append(profile.get("learningStyle")).append("\n");
            }
            sb.append("\n");
        }

        if (mastery != null && !mastery.isEmpty()) {
            sb.append("## 已有掌握情况（知识点ID -> 掌握度0~1）\n");
            mastery.forEach((kpId, level) ->
                    sb.append("- ").append(kpId).append("：").append(String.format("%.2f", level)).append("\n"));
            sb.append("\n");
            sb.append("请根据IRT自适应测试思想调整题目难度分布：\n");
            sb.append("- 掌握度低（<0.4）的知识点：出较多中低难度题，确认薄弱程度\n");
            sb.append("- 掌握度中等（0.4~0.7）的知识点：出中等难度题，精确定位水平\n");
            sb.append("- 掌握度高（>0.7）的知识点：出少量高难度题，验证是否真正掌握\n");
            sb.append("- 无历史记录的知识点：出中等难度题作为基线测试\n\n");
        } else {
            sb.append("该学生暂无历史数据，请生成覆盖各难度级别的基线诊断题目。\n\n");
        }

        // 画像驱动：按基础水平设定整体难度基线（无掌握度历史时尤其重要）
        String foundationLevel = sessionData.get("foundationLevel") != null
                ? String.valueOf(sessionData.get("foundationLevel"))
                : (profile != null ? String.valueOf(profile.getOrDefault("foundationLevel", "")) : "");
        if (foundationLevel != null && !foundationLevel.isBlank() && !"null".equals(foundationLevel)) {
            sb.append("## 难度基线（按学生基础水平）\n");
            if (foundationLevel.contains("较强") || foundationLevel.contains("扎实")
                    || foundationLevel.contains("进阶") || foundationLevel.contains("强")) {
                sb.append("该生基础『").append(foundationLevel).append("』：整体偏中高难度（difficulty 3-5 为主），")
                  .append("少量基础题确认无遗漏，多出综合应用与进阶题。\n\n");
            } else if (foundationLevel.contains("基础") || foundationLevel.contains("弱")
                    || foundationLevel.contains("入门") || foundationLevel.contains("差")) {
                sb.append("该生基础『").append(foundationLevel).append("』：整体偏中低难度（difficulty 1-3 为主），")
                  .append("先扎实核心概念与基本应用，避免一上来就高难度打击信心。\n\n");
            } else {
                sb.append("该生基础『").append(foundationLevel).append("』：难度以中等为主（difficulty 2-4），均衡覆盖。\n\n");
            }
        }

        // 画像驱动：薄弱模块加权出题
        Object weakObj = sessionData.get("weakModulePriority");
        String weakText = weakObj != null ? String.valueOf(weakObj) : "";
        if (weakText != null && !weakText.isBlank() && !"null".equals(weakText) && !"[]".equals(weakText.trim())) {
            sb.append("## 薄弱模块（重点覆盖）\n");
            sb.append("该生自述/历史薄弱：").append(weakText).append("。\n");
            sb.append("请让至少一半题目聚焦这些薄弱模块相关的知识点，优先暴露和定位短板。\n\n");
        }

        sb.append("## 输出要求\n");
        sb.append("返回 JSON：{\"questions\":[ ... ]}，每道题是一个对象，必须含 type 字段，按题型给字段：\n");
        sb.append("1) 单选题 type=\"choice\"：{\"id\":\"q1\",\"type\":\"choice\",\"knowledgePointId\":\"...\",\"knowledgePointName\":\"中文知识点名\",")
          .append("\"question\":\"题干\",\"options\":{\"A\":\"...\",\"B\":\"...\",\"C\":\"...\",\"D\":\"...\"},\"correctAnswer\":\"A\",\"difficulty\":3,\"errorType\":\"错误类型\"}\n");
        sb.append("2) 判断题 type=\"judge\"：{\"id\":\"q2\",\"type\":\"judge\",\"knowledgePointId\":\"...\",\"knowledgePointName\":\"...\",")
          .append("\"question\":\"题干\",\"options\":{\"A\":\"正确\",\"B\":\"错误\"},\"correctAnswer\":\"A\",\"difficulty\":2,\"errorType\":\"...\"}\n");
        sb.append("3) 填空题 type=\"fill\"：{\"id\":\"q3\",\"type\":\"fill\",\"knowledgePointId\":\"...\",\"knowledgePointName\":\"...\",")
          .append("\"question\":\"题干（用 ___ 表示空）\",\"referenceAnswer\":\"标准答案\",\"difficulty\":3,\"errorType\":\"...\"}\n");
        sb.append("4) 主观题 type=\"subjective\"：{\"id\":\"q4\",\"type\":\"subjective\",\"knowledgePointId\":\"...\",\"knowledgePointName\":\"...\",")
          .append("\"question\":\"题干\",\"referenceAnswer\":\"参考答案与评分要点\",\"difficulty\":4,\"errorType\":\"...\"}\n\n");
        sb.append("题型搭配：根据本科目特点合理混用题型（如语言/人文类可多用填空与主观题，理工类可多用单选与判断）；")
          .append("但客观题（单选/判断）应占多数，保证学生能快速作答，主观题不超过 2 道。\n");
        sb.append("共生成 ").append(TARGET_QUESTION_COUNT).append(" 道题，覆盖至少 ").append(MIN_QUESTION_COUNT).append(" 个知识点。");
        sb.append("difficulty 取 1-5，题目 id 从 q1 递增，每题必填 type 和 knowledgePointName。题干和选项要简洁。\n");

        return sb.toString();
    }

    /**
     * 解析题目生成的 LLM 返回。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseGenerateResponse(String llmResponse, AgentContext context) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> json = safeParseJson(llmResponse);

            List<Map<String, Object>> questions = null;
            Object questionsObj = json.get("questions");
            if (questionsObj instanceof List) {
                questions = (List<Map<String, Object>>) questionsObj;
            }

            if (questions == null || questions.isEmpty()) {
                // 尝试直接作为数组解析（LLM 可能直接返回数组）
                try {
                    String cleaned = llmResponse.replaceAll("(?s)```json\\s*", "")
                            .replaceAll("(?s)```\\s*", "").trim();
                    if (cleaned.startsWith("[")) {
                        questions = objectMapper.readValue(cleaned,
                                new TypeReference<List<Map<String, Object>>>() {});
                    }
                } catch (Exception ignored) {
                    // 解析失败则保持 null
                }
            }

            if (questions != null) {
                result.put("questions", questions);
                result.put("questionCount", questions.size());
            } else {
                log.warn("[{}] 无法从LLM响应中解析出题目列表", getName());
                result.put("questions", Collections.emptyList());
                result.put("questionCount", 0);
            }
        } catch (Exception e) {
            log.error("[{}] 解析题目生成结果失败: {}", getName(), e.getMessage());
            result.put("questions", Collections.emptyList());
            result.put("questionCount", 0);
        }
        return result;
    }

    /**
     * 校验生成的题目数量 >= {@link #MIN_QUESTION_COUNT}。
     */
    @SuppressWarnings("unchecked")
    private AgentResponse qualityCheckGenerate(Map<String, Object> parsed, AgentContext context) {
        int count = 0;
        Object countObj = parsed.get("questionCount");
        if (countObj instanceof Number) {
            count = ((Number) countObj).intValue();
        }

        if (count < MIN_QUESTION_COUNT) {
            log.warn("[{}] 题目生成质量不合格: 题数={}, 最低要求={}", getName(), count, MIN_QUESTION_COUNT);
            return AgentResponse.failure(
                    String.format("题目生成数量不足：实际 %d 题，要求至少 %d 题", count, MIN_QUESTION_COUNT));
        }

        // 将题目存入 sessionData 供后续 analyze 使用
        List<Map<String, Object>> questions = (List<Map<String, Object>>) parsed.get("questions");
        if (Boolean.TRUE.equals(context.getSessionData().get("customSubject"))) {
            String subject = context.getModule();
            int offTopic = countClearlyOffTopicQuestions(questions, subject);
            if (offTopic > 0) {
                log.warn("[{}] 自定义科目题目跑题: subject={}, offTopic={}", getName(), subject, offTopic);
                return AgentResponse.failure(
                        String.format("题目与诊断科目「%s」不匹配，请重新生成", subject));
            }
        }
        context.putSessionData("questions", questions);

        Map<String, Object> data = new HashMap<>(parsed);
        return AgentResponse.success(
                String.format("成功生成 %d 道诊断题目", count),
                data);
    }

    private int countClearlyOffTopicQuestions(List<Map<String, Object>> questions, String subject) {
        if (questions == null || questions.isEmpty()) {
            return 0;
        }
        List<String> forbiddenTerms = Arrays.asList(
                "Java", "Spring", "Servlet", "REST", "数据库", "SQL",
                "人工智能", "机器学习", "神经网络", "搜索算法",
                "数字电路", "逻辑门", "触发器", "时序电路");
        List<String> subjectTerms = subjectTerms(subject);
        int offTopic = 0;
        for (Map<String, Object> q : questions) {
            String text = String.join(" ",
                    String.valueOf(q.getOrDefault("knowledgePointId", "")),
                    String.valueOf(q.getOrDefault("knowledgePointName", "")),
                    String.valueOf(q.getOrDefault("question", "")),
                    String.valueOf(q.getOrDefault("options", "")),
                    String.valueOf(q.getOrDefault("referenceAnswer", "")));
            boolean forbidden = forbiddenTerms.stream().anyMatch(text::contains);
            boolean hasSubjectSignal = subjectTerms.isEmpty() || subjectTerms.stream().anyMatch(text::contains);
            if (forbidden && !hasSubjectSignal) {
                offTopic++;
            }
        }
        return offTopic;
    }

    private List<String> subjectTerms(String subject) {
        if (subject == null || subject.isBlank()) {
            return Collections.emptyList();
        }
        List<String> terms = new ArrayList<>();
        terms.add(subject);
        if (subject.contains("英语") || subject.toLowerCase(Locale.ROOT).contains("english")) {
            terms.addAll(Arrays.asList("英语", "English", "词汇", "语法", "阅读", "听力", "写作", "翻译", "句子", "篇章"));
        }
        return terms;
    }

    // ================================================================== analyze 逻辑

    /**
     * 构建答题分析 prompt。
     */
    @SuppressWarnings("unchecked")
    private String buildAnalyzePrompt(AgentContext context) {
        Map<String, Object> sessionData = context.getSessionData();

        // 获取题目和答案
        List<Map<String, Object>> questions = Collections.emptyList();
        Object qObj = sessionData.get("questions");
        if (qObj instanceof List) {
            questions = (List<Map<String, Object>>) qObj;
        }

        Map<String, String> answers = Collections.emptyMap();
        Object aObj = sessionData.get("answers");
        if (aObj instanceof Map) {
            answers = (Map<String, String>) aObj;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## 任务\n");
        sb.append("请分析以下学生的诊断测试答题结果，识别薄弱知识点和错误模式。\n\n");

        sb.append("## 模块：").append(context.getModule()).append("\n\n");

        sb.append("## 题目及学生作答\n");
        for (Map<String, Object> q : questions) {
            String qId = String.valueOf(q.get("id"));
            String studentAnswer = answers.getOrDefault(qId, "未作答");
            String correctAnswer = String.valueOf(q.get("correctAnswer"));
            boolean isCorrect = studentAnswer.equalsIgnoreCase(correctAnswer);

            sb.append("### 题目 ").append(qId).append("\n");
            sb.append("- 知识点：").append(q.get("knowledgePointId")).append("\n");
            sb.append("- 题目：").append(q.get("question")).append("\n");
            sb.append("- 选项：").append(q.get("options")).append("\n");
            sb.append("- 正确答案：").append(correctAnswer).append("\n");
            sb.append("- 学生答案：").append(studentAnswer).append("\n");
            sb.append("- 结果：").append(isCorrect ? "✓ 正确" : "✗ 错误").append("\n");
            sb.append("- 难度：").append(q.get("difficulty")).append("\n");
            sb.append("- 预期错误类型：").append(q.get("errorType")).append("\n\n");
        }

        sb.append("## 输出要求\n");
        sb.append("请以JSON格式返回分析结果，结构如下：\n");
        sb.append("```json\n");
        sb.append("{\n");
        sb.append("  \"score\": 75,\n");
        sb.append("  \"weakPoints\": [\n");
        sb.append("    {\n");
        sb.append("      \"kpId\": \"知识点ID\",\n");
        sb.append("      \"kpName\": \"知识点名称\",\n");
        sb.append("      \"masteryLevel\": 0.3\n");
        sb.append("    }\n");
        sb.append("  ],\n");
        sb.append("  \"errorPatterns\": {\n");
        sb.append("    \"错误模式描述\": 2\n");
        sb.append("  },\n");
        sb.append("  \"detailedAnalysis\": [\n");
        sb.append("    {\n");
        sb.append("      \"questionId\": \"q1\",\n");
        sb.append("      \"correct\": false,\n");
        sb.append("      \"errorType\": \"错误类型\",\n");
        sb.append("      \"analysis\": \"具体分析说明\"\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n");
        sb.append("```\n\n");
        sb.append("要求：\n");
        sb.append("1. score 为百分制得分（0-100）\n");
        sb.append("2. weakPoints 只包含掌握度低于0.6的薄弱知识点，masteryLevel 取值 0.0~1.0\n");
        sb.append("3. errorPatterns 的 key 为错误模式描述（如\"概念理解错误\"\"计算失误\"等），value 为出现次数\n");
        sb.append("4. detailedAnalysis 对每道题都给出分析\n");

        return sb.toString();
    }

    /**
     * 解析答题分析的 LLM 返回，并更新 AgentContext 中的 weakPoints 和 errorPatterns。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAnalyzeResponse(String llmResponse, AgentContext context) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> json = safeParseJson(llmResponse);

            // 分数
            Object scoreObj = json.get("score");
            int score = 0;
            if (scoreObj instanceof Number) {
                score = ((Number) scoreObj).intValue();
            }
            result.put("score", score);

            // 薄弱点
            List<Map<String, Object>> weakPoints = new ArrayList<>();
            Object wpObj = json.get("weakPoints");
            if (wpObj instanceof List) {
                weakPoints = (List<Map<String, Object>>) wpObj;
            }
            result.put("weakPoints", weakPoints);

            // 错误模式
            Map<String, Integer> errorPatterns = new HashMap<>();
            Object epObj = json.get("errorPatterns");
            if (epObj instanceof Map) {
                Map<String, Object> rawPatterns = (Map<String, Object>) epObj;
                for (Map.Entry<String, Object> entry : rawPatterns.entrySet()) {
                    int count = 1;
                    if (entry.getValue() instanceof Number) {
                        count = ((Number) entry.getValue()).intValue();
                    }
                    errorPatterns.put(entry.getKey(), count);
                }
            }
            result.put("errorPatterns", errorPatterns);

            // 详细分析
            Object daObj = json.get("detailedAnalysis");
            if (daObj instanceof List) {
                result.put("detailedAnalysis", daObj);
            } else {
                result.put("detailedAnalysis", Collections.emptyList());
            }

            // ---- 回写 AgentContext ----
            context.getWeakPoints().clear();
            context.getWeakPoints().addAll(weakPoints);

            context.getErrorPatterns().clear();
            context.getErrorPatterns().putAll(errorPatterns);

            // 同步更新知识点掌握度
            for (Map<String, Object> wp : weakPoints) {
                String kpId = String.valueOf(wp.get("kpId"));
                Object mlObj = wp.get("masteryLevel");
                double masteryLevel = 0.0;
                if (mlObj instanceof Number) {
                    masteryLevel = ((Number) mlObj).doubleValue();
                }
                context.updateMastery(kpId, masteryLevel);
            }

        } catch (Exception e) {
            log.error("[{}] 解析答题分析结果失败: {}", getName(), e.getMessage());
        }
        return result;
    }

    /**
     * 校验分析结果必须包含 weakPoints 信息。
     */
    @SuppressWarnings("unchecked")
    private AgentResponse qualityCheckAnalyze(Map<String, Object> parsed, AgentContext context) {
        List<Map<String, Object>> weakPoints = Collections.emptyList();
        Object wpObj = parsed.get("weakPoints");
        if (wpObj instanceof List) {
            weakPoints = (List<Map<String, Object>>) wpObj;
        }

        // 分析结果中 weakPoints 可以为空（学生全部掌握），但 parsed 本身应包含该字段
        if (!parsed.containsKey("weakPoints")) {
            log.warn("[{}] 分析结果缺少 weakPoints 字段", getName());
            return AgentResponse.failure("分析结果不完整：缺少薄弱知识点信息");
        }

        Object scoreObj = parsed.get("score");
        int score = scoreObj instanceof Number ? ((Number) scoreObj).intValue() : 0;

        Map<String, Object> data = new HashMap<>(parsed);
        String message = String.format("诊断完成：得分 %d 分，发现 %d 个薄弱知识点",
                score, weakPoints.size());

        return AgentResponse.success(message, data, AgentEvent.DIAGNOSIS_COMPLETE, "TracingAgent");
    }

    // ================================================================== 辅助方法

    /**
     * 从 sessionData 中解析当前动作类型，默认为 "generate"。
     */
    private String resolveAction(AgentContext context) {
        Object action = context.getSessionData().get("action");
        if (action instanceof String) {
            return (String) action;
        }
        return "generate";
    }
}
