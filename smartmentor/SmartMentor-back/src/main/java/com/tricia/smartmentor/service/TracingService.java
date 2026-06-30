package com.tricia.smartmentor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tricia.smartmentor.agent.*;
import com.tricia.smartmentor.common.BusinessException;
import com.tricia.smartmentor.entity.AnswerRecord;
import com.tricia.smartmentor.entity.DiagnosticSession;
import com.tricia.smartmentor.entity.TracingResult;
import com.tricia.smartmentor.repository.AnswerRecordRepository;
import com.tricia.smartmentor.repository.DiagnosticSessionRepository;
import com.tricia.smartmentor.repository.TracingResultRepository;
import com.tricia.smartmentor.service.KnowledgeGraphService.KnowledgeNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TracingService {

    private final TracingResultRepository tracingResultRepository;
    private final DiagnosticSessionRepository diagnosticSessionRepository;
    private final AnswerRecordRepository answerRecordRepository;
    private final TracingAgent tracingAgent;
    private final AgentOrchestrator orchestrator;
    private final KnowledgeGraphService knowledgeGraphService;
    private final ObjectMapper objectMapper;
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(2);

    public TracingService(TracingResultRepository tracingResultRepository,
                          DiagnosticSessionRepository diagnosticSessionRepository,
                          AnswerRecordRepository answerRecordRepository,
                          TracingAgent tracingAgent,
                          AgentOrchestrator orchestrator,
                          KnowledgeGraphService knowledgeGraphService,
                          ObjectMapper objectMapper) {
        this.tracingResultRepository = tracingResultRepository;
        this.diagnosticSessionRepository = diagnosticSessionRepository;
        this.answerRecordRepository = answerRecordRepository;
        this.tracingAgent = tracingAgent;
        this.orchestrator = orchestrator;
        this.knowledgeGraphService = knowledgeGraphService;
        this.objectMapper = objectMapper;
    }

    /**
     * Analyze knowledge tracing
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> analyze(Long studentId, String diagnosticId, List<String> knowledgePointIds,
                                        Integer maxDepth, Double masteryThreshold) {
        if ((diagnosticId == null || diagnosticId.isEmpty()) && (knowledgePointIds == null || knowledgePointIds.isEmpty())) {
            throw new BusinessException(400, "必须提供 diagnosticId 或 knowledgePointIds");
        }

        int depth = maxDepth != null ? maxDepth : 3;
        double threshold = masteryThreshold != null ? masteryThreshold : 0.6;

        // ---- 1. 收集薄弱知识点 ----
        List<Map<String, Object>> weakPoints = new ArrayList<>();
        Map<String, Double> masteryMap = new HashMap<>();
        Map<String, Integer> errorPatterns = new HashMap<>();
        String module = null;

        if (diagnosticId != null && !diagnosticId.isEmpty()) {
            DiagnosticSession session = diagnosticSessionRepository.findByDiagnosticId(diagnosticId)
                    .orElseThrow(() -> new BusinessException(404, "诊断会话不存在"));
            if (!session.getStudentId().equals(studentId)) {
                throw new BusinessException(403, "无权访问此诊断会话");
            }
            module = session.getModule();

            List<AnswerRecord> records = answerRecordRepository.findByDiagnosticIdOrderByQuestionIndexAsc(diagnosticId);
            // 先按知识点统计真实作答情况（答对数/总数），用于计算真实掌握度，而非写死常数
            Map<String, int[]> kpStats = new LinkedHashMap<>(); // kpId -> [correct, total]
            for (AnswerRecord r : records) {
                String kpId = r.getKnowledgePointId();
                if (kpId == null) {
                    continue;
                }
                int[] stat = kpStats.computeIfAbsent(kpId, k -> new int[2]);
                if (Boolean.TRUE.equals(r.getIsCorrect())) {
                    stat[0]++;
                }
                stat[1]++;
            }
            Set<String> seen = new LinkedHashSet<>();
            for (AnswerRecord r : records) {
                String kpId = r.getKnowledgePointId();
                if (Boolean.FALSE.equals(r.getIsCorrect()) && kpId != null && seen.add(kpId)) {
                    int[] stat = kpStats.get(kpId);
                    double realMastery = (stat != null && stat[1] > 0)
                            ? round2((double) stat[0] / stat[1]) : 0.3;
                    Map<String, Object> wp = new LinkedHashMap<>();
                    wp.put("kpId", kpId);
                    wp.put("kpName", resolveKpName(kpId, r.getKnowledgePointName()));
                    wp.put("masteryLevel", realMastery);
                    weakPoints.add(wp);
                    masteryMap.put(kpId, realMastery);
                }
                // 统计错误模式
                if (Boolean.FALSE.equals(r.getIsCorrect()) && r.getErrorType() != null && !r.getErrorType().isEmpty()) {
                    errorPatterns.merge(r.getErrorType(), 1, Integer::sum);
                }
            }
        }

        if (knowledgePointIds != null) {
            Set<String> existingIds = weakPoints.stream()
                    .map(wp -> (String) wp.get("kpId"))
                    .collect(Collectors.toSet());
            for (String rawKpId : knowledgePointIds) {
                String kpId = normalizeKnowledgePointId(rawKpId);
                if (kpId != null && !existingIds.contains(kpId)) {
                    Map<String, Object> wp = new LinkedHashMap<>();
                    wp.put("kpId", kpId);
                    wp.put("kpName", resolveKpName(kpId, null));
                    wp.put("masteryLevel", 0.3);
                    weakPoints.add(wp);
                    masteryMap.put(kpId, 0.3);
                }
            }
        }

        if (weakPoints.isEmpty()) {
            weakPoints.add(Map.of("kpId", "unknown", "kpName", "课程基础", "masteryLevel", 0.3));
        }

        // ---- 2. 先用本地知识图谱快速生成回退结果 ----
        List<String> weakKpIds = weakPoints.stream()
                .map(wp -> normalizeKnowledgePointId((String) wp.get("kpId")))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Set<String> allRootCauseIds = fallbackRootCauses(weakKpIds);
        List<Map<String, Object>> tracingResults = buildTracingResultsFromGraph(weakPoints, allRootCauseIds, masteryMap, depth, threshold);
        List<Map<String, Object>> mergedRootCauses = buildMergedRootCauses(allRootCauseIds, masteryMap, null);
        Map<String, Object> graphVisualization = buildGraphVisualization(weakKpIds, allRootCauseIds, depth);
        List<Map<String, Object>> suggestedLearningPath = buildLearningPath(allRootCauseIds, weakKpIds);
        String suggestion = buildFallbackSuggestion(weakPoints, mergedRootCauses);

        // ---- 3. 持久化（先保存本地回退结果，标记AI待分析） ----
        String tracingId = "trace_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "_" + UUID.randomUUID().toString().substring(0, 8);

        boolean isCrossModule = checkCrossModule(weakKpIds, allRootCauseIds);

        TracingResult entity = new TracingResult();
        entity.setTracingId(tracingId);
        entity.setStudentId(studentId);
        entity.setDiagnosticId(diagnosticId);
        entity.setAnalyzedPointCount(weakPoints.size());
        entity.setRootCauseCount(allRootCauseIds.size());
        entity.setIsCrossModule(isCrossModule);
        entity.setTracingResults(toJson(tracingResults));
        entity.setMergedRootCauses(toJson(mergedRootCauses));
        entity.setGraphVisualization(toJson(graphVisualization));
        entity.setSuggestedLearningPath(toJson(suggestedLearningPath));
        entity.setSuggestion(suggestion);
        tracingResultRepository.save(entity);

        // ---- 4. 异步提交 AI 根因分析，完成后更新数据库 ----
        final String fModule = module;
        final List<Map<String, Object>> fWeakPoints = new ArrayList<>(weakPoints);
        final Map<String, Double> fMasteryMap = new HashMap<>(masteryMap);
        final Map<String, Integer> fErrorPatterns = new HashMap<>(errorPatterns);
        final List<String> fWeakKpIds = new ArrayList<>(weakKpIds);
        final String fDiagnosticId = diagnosticId;

        asyncExecutor.submit(() -> {
            try {
                runAsyncAITracing(tracingId, studentId, fModule, fDiagnosticId,
                        fWeakPoints, fMasteryMap, fErrorPatterns, fWeakKpIds, depth, threshold);
            } catch (Exception e) {
                log.error("异步溯因AI分析失败, tracingId={}: {}", tracingId, e.getMessage(), e);
            }
        });

        // ---- 5. 立即返回响应（含本地回退数据 + aiPending标志） ----
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tracingId", tracingId);
        result.put("diagnosticId", diagnosticId);
        result.put("createdAt", LocalDateTime.now().toString());
        result.put("analyzedPointCount", weakPoints.size());
        result.put("rootCauseCount", allRootCauseIds.size());
        result.put("isCrossModule", isCrossModule);
        result.put("aiAnalysisPending", true);

        List<Map<String, Object>> cleanResults = new ArrayList<>();
        for (Map<String, Object> tr : tracingResults) {
            Map<String, Object> clean = new LinkedHashMap<>(tr);
            clean.remove("pathNodes");
            cleanResults.add(clean);
        }
        result.put("tracingResults", cleanResults);
        result.put("mergedRootCauses", mergedRootCauses);
        result.put("suggestion", suggestion);
        return result;
    }

    /**
     * Get full tracing result
     */
    public Map<String, Object> getTracingResult(Long studentId, String tracingId) {
        TracingResult entity = tracingResultRepository.findByTracingId(tracingId)
                .orElseThrow(() -> new BusinessException(404, "溯源结果不存在"));

        if (!entity.getStudentId().equals(studentId)) {
            throw new BusinessException(403, "无权查看此溯源结果");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tracingId", entity.getTracingId());
        result.put("diagnosticId", entity.getDiagnosticId());
        result.put("analyzedPointCount", entity.getAnalyzedPointCount());
        result.put("rootCauseCount", entity.getRootCauseCount());
        result.put("isCrossModule", entity.getIsCrossModule());
        result.put("tracingResults", parseJson(entity.getTracingResults()));
        result.put("mergedRootCauses", parseJson(entity.getMergedRootCauses()));
        result.put("graphVisualization", parseJson(entity.getGraphVisualization()));
        result.put("suggestedLearningPath", parseJson(entity.getSuggestedLearningPath()));
        result.put("suggestion", entity.getSuggestion());
        result.put("createdAt", entity.getCreatedAt());
        // AI分析完成标志：suggestion中包含"【AI 分析】"表示AI已完成
        boolean aiDone = entity.getSuggestion() != null && entity.getSuggestion().contains("【AI 分析】");
        result.put("aiAnalysisPending", !aiDone);
        return result;
    }

    /**
     * 异步执行 AI 根因分析，完成后更新数据库中的 TracingResult
     */
    @SuppressWarnings("unchecked")
    private void runAsyncAITracing(String tracingId, Long studentId, String module,
                                    String diagnosticId,
                                    List<Map<String, Object>> weakPoints,
                                    Map<String, Double> masteryMap,
                                    Map<String, Integer> errorPatterns,
                                    List<String> weakKpIds,
                                    int depth, double threshold) {
        log.info("开始异步AI溯因分析, tracingId={}", tracingId);
        long start = System.currentTimeMillis();

        try {
            // 构建知识图谱上下文
            String knowledgeGraphContext = buildKnowledgeGraphContext(weakKpIds, depth);

            // 构建 AgentContext
            AgentContext agentContext = AgentContext.builder()
                    .studentId(studentId)
                    .module(module != null ? module : "高校课程")
                    .build();

            agentContext.getWeakPoints().addAll(weakPoints);
            agentContext.getKnowledgeMastery().putAll(masteryMap);
            agentContext.getErrorPatterns().putAll(errorPatterns);
            agentContext.putSessionData("knowledgeGraph", knowledgeGraphContext);

            // 通过编排器触发 DIAGNOSIS_COMPLETE 事件链：TracingAgent 作为该事件的注册处理器被调用。
            // 用 NoCascade——溯源完成后 TracingAgent 会产出 TRACING_COMPLETE，但路径规划属独立 HTTP 端点，
            // 不应在本溯源请求内被连带触发，故截断级联。
            List<AgentResponse> responses = orchestrator.fireEventNoCascade(AgentEvent.DIAGNOSIS_COMPLETE, agentContext);
            AgentResponse response = responses.isEmpty()
                    ? AgentResponse.failure("溯源未产出结果")
                    : responses.get(0);

            if (response.isSuccess() && response.getData() != null) {
                Map<String, Object> aiData = response.getData();
                String aiNarrative = (String) aiData.get("analysisNarrative");

                // 合并 AI 根因与本地根因
                Set<String> allRootCauseIds = fallbackRootCauses(weakKpIds);
                List<Map<String, Object>> aiRootCauses = (List<Map<String, Object>>) aiData.get("rootCauses");
                if (aiRootCauses != null) {
                    for (Map<String, Object> arc : aiRootCauses) {
                        Object nodeId = arc.get("nodeId");
                        String normalizedNodeId = normalizeKnowledgePointId(nodeId);
                        if (normalizedNodeId == null) {
                            continue;
                        }
                        // M7：仅接受真实存在于知识图谱中的节点，丢弃 LLM 幻觉出的图谱外根因
                        if (knowledgeGraphService.getNode(normalizedNodeId) != null) {
                            allRootCauseIds.add(normalizedNodeId);
                        } else {
                            log.warn("溯源: AI 返回图谱外根因 nodeId={}，已丢弃 (tracingId={})", normalizedNodeId, tracingId);
                        }
                    }
                }

                // 重新构建增强结果
                List<Map<String, Object>> tracingResults = buildTracingResultsFromGraph(weakPoints, allRootCauseIds, masteryMap, depth, threshold);
                List<Map<String, Object>> mergedRootCauses = buildMergedRootCauses(allRootCauseIds, masteryMap, aiData);
                Map<String, Object> graphVisualization = buildGraphVisualization(weakKpIds, allRootCauseIds, depth);
                List<Map<String, Object>> suggestedLearningPath = buildLearningPath(allRootCauseIds, weakKpIds);
                String suggestion = buildSuggestionFromAI(aiNarrative, weakPoints, mergedRootCauses);
                boolean isCrossModule = checkCrossModule(weakKpIds, allRootCauseIds);

                // 更新数据库
                tracingResultRepository.findByTracingId(tracingId).ifPresent(entity -> {
                    entity.setRootCauseCount(allRootCauseIds.size());
                    entity.setIsCrossModule(isCrossModule);
                    entity.setTracingResults(toJson(tracingResults));
                    entity.setMergedRootCauses(toJson(mergedRootCauses));
                    entity.setGraphVisualization(toJson(graphVisualization));
                    entity.setSuggestedLearningPath(toJson(suggestedLearningPath));
                    entity.setSuggestion(suggestion);
                    tracingResultRepository.save(entity);
                    log.info("异步AI溯因分析完成, tracingId={}, 耗时{}ms", tracingId, System.currentTimeMillis() - start);
                });
            } else {
                log.warn("AI溯因分析返回失败, tracingId={}, msg={}", tracingId, response.getMessage());
            }
        } catch (Exception e) {
            log.error("异步AI溯因分析异常, tracingId={}: {}", tracingId, e.getMessage(), e);
        }
    }

    // ================================================================== 知识图谱上下文构建

    /**
     * 为 TracingAgent 构建知识图谱上下文字符串
     */
    private String buildKnowledgeGraphContext(List<String> weakKpIds, int depth) {
        StringBuilder sb = new StringBuilder();
        Set<String> mentioned = new HashSet<>();

        for (String kpId : weakKpIds) {
            KnowledgeNode node = knowledgeGraphService.getNode(kpId);
            if (node == null) continue;

            sb.append("知识点: ").append(node.getId())
                    .append(" (").append(node.getName()).append(")")
                    .append(" 模块=").append(node.getModule())
                    .append(" 描述=").append(node.getDescription() != null ? node.getDescription() : "")
                    .append("\n");
            mentioned.add(kpId);

            // 添加前置依赖链
            List<KnowledgeNode> prereqs = knowledgeGraphService.getAllPrerequisitesRecursive(kpId);
            for (KnowledgeNode prereq : prereqs) {
                if (mentioned.add(prereq.getId())) {
                    sb.append("  前置: ").append(prereq.getId())
                            .append(" (").append(prereq.getName()).append(")")
                            .append(" 模块=").append(prereq.getModule())
                            .append("\n");
                }
            }

            // 添加直接前置的边关系
            List<KnowledgeNode> directPrereqs = knowledgeGraphService.getPrerequisites(kpId);
            for (KnowledgeNode dp : directPrereqs) {
                sb.append("  依赖边: ").append(kpId).append(" -> ").append(dp.getId()).append("\n");
            }
            sb.append("\n");
        }

        if (sb.length() == 0) {
            sb.append("暂无知识图谱数据，请基于教学经验推断可能的知识依赖关系\n");
        }
        return sb.toString();
    }

    // ================================================================== 溯源结果构建（基于真实知识图谱）

    private List<Map<String, Object>> buildTracingResultsFromGraph(
            List<Map<String, Object>> weakPoints,
            Set<String> rootCauseIds,
            Map<String, Double> masteryMap,
            int maxDepth, double threshold) {

        List<Map<String, Object>> tracingResults = new ArrayList<>();

        for (Map<String, Object> wp : weakPoints) {
            String kpId = (String) wp.get("kpId");
            String kpName = (String) wp.get("kpName");
            double mastery = wp.get("masteryLevel") instanceof Number
                    ? ((Number) wp.get("masteryLevel")).doubleValue() : 0.3;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("targetPointId", kpId);
            item.put("targetPointName", kpName);
            item.put("targetMastery", mastery);

            // 基于知识图谱构建溯源路径
            List<Map<String, Object>> tracingPath = new ArrayList<>();
            List<Map<String, Object>> pathEdges = new ArrayList<>();
            List<Map<String, Object>> pathNodes = new ArrayList<>();

            // 目标节点
            KnowledgeNode targetNode = knowledgeGraphService.getNode(kpId);
            String targetModule = targetNode != null ? targetNode.getModule() : "";
            Map<String, Object> targetNodeMap = buildNodeMap(kpId, kpName, mastery, 0, targetModule, true, false, false);
            tracingPath.add(targetNodeMap);
            pathNodes.add(targetNodeMap);

            // 从知识图谱获取真实前置依赖
            List<KnowledgeNode> prereqs = knowledgeGraphService.getPrerequisites(kpId);
            Map<String, Object> rootCauseObj = null;
            int depthCounter = 1;

            // BFS 展开前置链，受 maxDepth 限制
            Queue<String[]> bfsQueue = new LinkedList<>();
            Set<String> visited = new HashSet<>();
            visited.add(kpId);

            for (KnowledgeNode prereq : prereqs) {
                if (visited.add(prereq.getId())) {
                    bfsQueue.add(new String[]{kpId, prereq.getId(), String.valueOf(depthCounter)});
                }
            }

            while (!bfsQueue.isEmpty()) {
                String[] entry = bfsQueue.poll();
                String fromId = entry[0];
                String toId = entry[1];
                int currentDepth = Integer.parseInt(entry[2]);

                if (currentDepth > maxDepth) continue;

                KnowledgeNode toNode = knowledgeGraphService.getNode(toId);
                if (toNode == null) continue;

                // M9：优先用真实掌握度；无作答数据的前置点标记为"估计"（不伪造精确小数）
                Double realPrereqMastery = masteryMap != null ? masteryMap.get(toId) : null;
                double prereqMastery = realPrereqMastery != null ? realPrereqMastery : 0.3;
                boolean prereqEstimated = realPrereqMastery == null;
                Map<String, Object> prereqNodeMap = buildNodeMap(
                        toId, toNode.getName(), prereqMastery, currentDepth,
                        toNode.getModule(), false, rootCauseIds.contains(toId), prereqEstimated);
                tracingPath.add(prereqNodeMap);
                pathNodes.add(prereqNodeMap);

                boolean crossModule = !Objects.equals(
                        getModuleForId(fromId), toNode.getModule());
                Map<String, Object> edge = new LinkedHashMap<>();
                edge.put("from", fromId);
                edge.put("to", toId);
                edge.put("relation", "PREREQUISITE_OF");
                edge.put("weight", Math.round((0.9 - currentDepth * 0.1) * 10.0) / 10.0);
                edge.put("type", crossModule ? "cross_module" : "strong");
                pathEdges.add(edge);

                // M10：在所有命中的根因里选 depth 最深者（最底层根因），而非 BFS 首个命中（最浅前置）
                if (rootCauseIds.contains(toId)
                        && (rootCauseObj == null
                            || currentDepth > ((Number) rootCauseObj.get("depth")).intValue())) {
                    rootCauseObj = new LinkedHashMap<>();
                    rootCauseObj.put("knowledgePointId", toId);
                    rootCauseObj.put("knowledgePointName", toNode.getName());
                    rootCauseObj.put("module", toNode.getModule());
                    rootCauseObj.put("mastery", round2(prereqMastery));
                    rootCauseObj.put("masteryEstimated", prereqEstimated);
                    rootCauseObj.put("depth", currentDepth);
                    // M6：如实描述该 reason 来自确定性图谱回溯；AI 贡献的 reason 单独存于 mergedRootCauses
                    rootCauseObj.put("reason", "基于知识图谱前置依赖回溯，该知识点为多个薄弱点的共同前置根节点，其掌握不足会连锁影响后续依赖知识点。");
                }

                // 继续展开下一层
                List<KnowledgeNode> nextPrereqs = knowledgeGraphService.getPrerequisites(toId);
                for (KnowledgeNode np : nextPrereqs) {
                    if (visited.add(np.getId())) {
                        bfsQueue.add(new String[]{toId, np.getId(), String.valueOf(currentDepth + 1)});
                    }
                }
            }

            item.put("rootCause", rootCauseObj);
            item.put("tracingPath", tracingPath);
            item.put("pathEdges", pathEdges);
            item.put("pathNodes", pathNodes);
            item.put("depth", Math.min(depthCounter, maxDepth));
            tracingResults.add(item);
        }

        return tracingResults;
    }

    private Map<String, Object> buildNodeMap(String id, String name, double mastery,
                                              int depth, String module,
                                              boolean isTarget, boolean isRootCause,
                                              boolean masteryEstimated) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("knowledgePointId", id);
        node.put("knowledgePointName", name);
        node.put("mastery", round2(mastery));
        node.put("masteryEstimated", masteryEstimated);
        node.put("depth", depth);
        node.put("module", module != null ? module : "");
        String statusStr;
        if (masteryEstimated) statusStr = "未测";
        else if (mastery >= 0.9) statusStr = "熟练";
        else if (mastery >= 0.7) statusStr = "基本掌握";
        else if (mastery >= 0.5) statusStr = "一般";
        else statusStr = "薄弱";
        node.put("status", statusStr);
        node.put("isTarget", isTarget);
        node.put("isRootCause", isRootCause);
        return node;
    }

    /** 掌握度统一保留两位小数。 */
    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    // ================================================================== 合并根因 & 学习路径

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildMergedRootCauses(Set<String> rootCauseIds, Map<String, Double> masteryMap, Map<String, Object> aiData) {
        List<Map<String, Object>> causes = new ArrayList<>();
        int priority = 1;

        // 从 AI 结果获取详细根因信息
        List<Map<String, Object>> aiRootCauses = null;
        if (aiData != null) {
            Object rcObj = aiData.get("rootCauses");
            if (rcObj instanceof List) {
                aiRootCauses = (List<Map<String, Object>>) rcObj;
            }
        }

        for (String rcId : sanitizeKnowledgePointIds(rootCauseIds)) {
            Map<String, Object> cause = new LinkedHashMap<>();
            cause.put("knowledgePointId", rcId);

            KnowledgeNode node = knowledgeGraphService.getNode(rcId);
            cause.put("knowledgePointName", node != null ? node.getName() : rcId);
            cause.put("module", node != null ? node.getModule() : "");
            // M9：根因若有真实作答掌握度则用之，否则标记为未测（不伪造 0.25）
            Double realMastery = masteryMap != null ? masteryMap.get(rcId) : null;
            cause.put("mastery", realMastery != null ? round2(realMastery) : null);
            cause.put("masteryEstimated", realMastery == null);

            // 从 AI 获取 confidence 和 reason
            if (aiRootCauses != null) {
                for (Map<String, Object> arc : aiRootCauses) {
                    if (Objects.equals(rcId, normalizeKnowledgePointId(arc.get("nodeId")))) {
                        cause.put("reason", arc.getOrDefault("reason", ""));
                        cause.put("confidence", arc.getOrDefault("confidence", 0.8));
                        break;
                    }
                }
            }
            cause.putIfAbsent("reason", "该知识点掌握不足，影响后续知识学习");
            cause.putIfAbsent("confidence", 0.7);

            // 使用知识图谱获取受影响的节点
            List<KnowledgeNode> dependents = knowledgeGraphService.getDependents(rcId);
            List<String> affectedIds = new ArrayList<>();
            List<String> affectedNames = new ArrayList<>();
            for (KnowledgeNode dep : dependents) {
                affectedIds.add(dep.getId());
                affectedNames.add(dep.getName());
            }
            cause.put("affectedPoints", affectedIds);
            cause.put("affectedPointNames", affectedNames);
            cause.put("priority", priority++);
            causes.add(cause);
        }
        return causes;
    }

    private List<Map<String, Object>> buildLearningPath(Set<String> rootCauseIds, List<String> weakKpIds) {
        Set<String> sanitizedRootCauseIds = sanitizeKnowledgePointIds(rootCauseIds);
        Set<String> sanitizedWeakKpIds = sanitizeKnowledgePointIds(weakKpIds);
        Set<String> allNodeIds = new LinkedHashSet<>();
        for (String id : sanitizedRootCauseIds) {
            addNodeAndPrerequisites(allNodeIds, id);
        }
        for (String id : sanitizedWeakKpIds) {
            addNodeAndPrerequisites(allNodeIds, id);
        }

        List<KnowledgeNode> sorted = knowledgeGraphService.topologicalSort(new ArrayList<>(allNodeIds));

        List<Map<String, Object>> path = new ArrayList<>();
        int order = 1;
        for (KnowledgeNode node : sorted) {
            Map<String, Object> step = new LinkedHashMap<>();
            step.put("order", order++);
            step.put("knowledgePointId", node.getId());
            step.put("knowledgePointName", node.getName());
            step.put("module", node.getModule());

            if (sanitizedRootCauseIds.contains(node.getId())) {
                step.put("phase", "巩固基础");
                step.put("estimatedTime", "30分钟");
                step.put("resources", Arrays.asList("概念讲解视频", "基础练习题"));
            } else {
                step.put("phase", "专项突破");
                step.put("estimatedTime", "45分钟");
                step.put("resources", Arrays.asList("知识点精讲", "典型例题", "变式练习"));
            }
            path.add(step);
        }

        // 综合复习步骤
        Map<String, Object> reviewStep = new LinkedHashMap<>();
        reviewStep.put("order", order);
        reviewStep.put("knowledgePointId", "review");
        reviewStep.put("knowledgePointName", "综合巩固检测");
        reviewStep.put("module", "综合");
        reviewStep.put("phase", "综合检测");
        reviewStep.put("estimatedTime", "60分钟");
        reviewStep.put("resources", Arrays.asList("综合测试卷", "易错题回顾"));
        path.add(reviewStep);

        return path;
    }

    private void addNodeAndPrerequisites(Set<String> allNodeIds, String id) {
        if (id == null || id.isBlank() || "unknown".equals(id) || "review".equals(id)) {
            return;
        }
        for (KnowledgeNode prereq : knowledgeGraphService.getAllPrerequisitesRecursive(id)) {
            allNodeIds.add(prereq.getId());
        }
        allNodeIds.add(id);
    }

    // ================================================================== 图可视化

    private Map<String, Object> buildGraphVisualization(List<String> weakKpIds,
                                                         Set<String> rootCauseIds, int depth) {
        Set<String> sanitizedWeakKpIds = sanitizeKnowledgePointIds(weakKpIds);
        Set<String> sanitizedRootCauseIds = sanitizeKnowledgePointIds(rootCauseIds);
        Set<String> allNodeIds = new LinkedHashSet<>(sanitizedWeakKpIds);
        allNodeIds.addAll(sanitizedRootCauseIds);

        // 收集所有涉及的节点及其前置
        Set<String> expandedIds = new LinkedHashSet<>(allNodeIds);
        for (String id : allNodeIds) {
            List<KnowledgeNode> prereqs = knowledgeGraphService.getAllPrerequisitesRecursive(id);
            for (KnowledgeNode p : prereqs) {
                expandedIds.add(p.getId());
            }
        }

        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        Set<String> edgeSet = new HashSet<>();

        for (String nid : expandedIds) {
            KnowledgeNode kn = knowledgeGraphService.getNode(nid);
            if (kn == null) continue;

            Map<String, Object> nodeMap = new LinkedHashMap<>();
            nodeMap.put("knowledgePointId", kn.getId());
            nodeMap.put("knowledgePointName", kn.getName());
            nodeMap.put("module", kn.getModule());
            nodeMap.put("isTarget", sanitizedWeakKpIds.contains(nid));
            nodeMap.put("isRootCause", sanitizedRootCauseIds.contains(nid));
            nodes.add(nodeMap);

            // 添加边
            List<KnowledgeNode> prereqs = knowledgeGraphService.getPrerequisites(nid);
            for (KnowledgeNode p : prereqs) {
                if (expandedIds.contains(p.getId())) {
                    String edgeKey = nid + "->" + p.getId();
                    if (edgeSet.add(edgeKey)) {
                        Map<String, Object> edge = new LinkedHashMap<>();
                        edge.put("from", nid);
                        edge.put("to", p.getId());
                        edge.put("relation", "PREREQUISITE_OF");
                        boolean cross = !Objects.equals(kn.getModule(), p.getModule());
                        edge.put("type", cross ? "cross_module" : "strong");
                        edges.add(edge);
                    }
                }
            }
        }

        Map<String, Object> graph = new LinkedHashMap<>();
        graph.put("nodes", nodes);
        graph.put("edges", edges);
        return graph;
    }

    // ================================================================== AI 建议生成

    private String buildSuggestionFromAI(String aiNarrative,
                                          List<Map<String, Object>> weakPoints,
                                          List<Map<String, Object>> rootCauses) {
        StringBuilder sb = new StringBuilder();
        sb.append("【知识溯源分析报告】\n\n");
        sb.append("本次分析了 ").append(weakPoints.size()).append(" 个薄弱知识点");

        List<String> wpNames = weakPoints.stream()
                .map(wp -> (String) wp.get("kpName"))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (!wpNames.isEmpty()) {
            sb.append("：").append(String.join("、", wpNames));
        }
        sb.append("。\n\n");

        if (aiNarrative != null && !aiNarrative.isEmpty()) {
            sb.append("【AI 分析】\n").append(aiNarrative).append("\n\n");
        }

        if (!rootCauses.isEmpty()) {
            sb.append("溯源发现 ").append(rootCauses.size()).append(" 个根本原因：\n");
            for (Map<String, Object> cause : rootCauses) {
                sb.append("- ").append(cause.get("knowledgePointName"));
                Object confidence = cause.get("confidence");
                if (confidence != null) {
                    sb.append("（置信度：").append(confidence).append("）");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        sb.append("建议按照推荐学习路径，从基础前置知识开始逐步巩固，");
        sb.append("确保每个环节掌握后再进入下一阶段的学习。");
        int estimatedMinutes = rootCauses.size() * 30 + weakPoints.size() * 45 + 60;
        sb.append("预计总学习时间约 ").append(estimatedMinutes).append(" 分钟。");
        return sb.toString();
    }

    // ================================================================== 本地回退

    /**
     * AI 失败时用知识图谱做本地根因分析
     */
    private Set<String> fallbackRootCauses(List<String> weakKpIds) {
        List<String> sanitizedWeakKpIds = new ArrayList<>(sanitizeKnowledgePointIds(weakKpIds));
        List<KnowledgeNode> roots = knowledgeGraphService.findRootCauses(sanitizedWeakKpIds);
        if (!roots.isEmpty()) {
            return roots.stream()
                    .map(KnowledgeNode::getId)
                    .map(this::normalizeKnowledgePointId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        // 如果图谱也找不到，用直接前置作为根因
        Set<String> fallback = new LinkedHashSet<>();
        for (String kpId : sanitizedWeakKpIds) {
            List<KnowledgeNode> prereqs = knowledgeGraphService.getPrerequisites(kpId);
            for (KnowledgeNode p : prereqs) {
                String prereqId = normalizeKnowledgePointId(p.getId());
                if (prereqId != null) {
                    fallback.add(prereqId);
                }
            }
        }
        if (fallback.isEmpty()) {
            fallback.addAll(sanitizedWeakKpIds);
        }
        return fallback;
    }

    private String buildFallbackSuggestion(List<Map<String, Object>> weakPoints,
                                            List<Map<String, Object>> rootCauses) {
        return buildSuggestionFromAI(null, weakPoints, rootCauses);
    }

    // ================================================================== 辅助方法

    private String resolveKpName(String kpId, String fallbackName) {
        KnowledgeNode node = knowledgeGraphService.getNode(kpId);
        if (node != null) {
            return node.getName();
        }
        return fallbackName != null ? fallbackName : kpId;
    }

    private String getModuleForId(String kpId) {
        KnowledgeNode node = knowledgeGraphService.getNode(kpId);
        return node != null ? node.getModule() : "";
    }

    private boolean checkCrossModule(List<String> weakKpIds, Set<String> rootCauseIds) {
        Set<String> modules = new HashSet<>();
        for (String id : sanitizeKnowledgePointIds(weakKpIds)) {
            modules.add(getModuleForId(id));
        }
        for (String id : sanitizeKnowledgePointIds(rootCauseIds)) {
            modules.add(getModuleForId(id));
        }
        modules.remove("");
        return modules.size() > 1;
    }

    private Set<String> sanitizeKnowledgePointIds(Collection<String> ids) {
        if (ids == null) {
            return Collections.emptySet();
        }
        return ids.stream()
                .map(this::normalizeKnowledgePointId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizeKnowledgePointId(Object id) {
        if (id == null) {
            return null;
        }
        String normalized = id.toString().trim();
        if (normalized.isEmpty()
                || "null".equalsIgnoreCase(normalized)
                || "undefined".equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized;
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
