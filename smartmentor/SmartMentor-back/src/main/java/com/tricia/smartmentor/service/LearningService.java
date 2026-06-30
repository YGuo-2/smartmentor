package com.tricia.smartmentor.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tricia.smartmentor.agent.*;
import com.tricia.smartmentor.common.BusinessException;
import com.tricia.smartmentor.entity.DiagnosticSession;
import com.tricia.smartmentor.entity.LearningPath;
import com.tricia.smartmentor.entity.TracingResult;
import com.tricia.smartmentor.repository.DiagnosticSessionRepository;
import com.tricia.smartmentor.repository.LearningPathRepository;
import com.tricia.smartmentor.repository.TracingResultRepository;
import com.tricia.smartmentor.entity.StudentProfile;
import com.tricia.smartmentor.repository.StudentProfileRepository;
import com.tricia.smartmentor.service.KnowledgeGraphService.KnowledgeNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LearningService {

    private final LearningPathRepository learningPathRepository;
    private final TracingResultRepository tracingResultRepository;
    private final DiagnosticSessionRepository diagnosticSessionRepository;
    private final KnowledgeGraphService knowledgeGraphService;
    private final PlanningAgent planningAgent;
    private final TeachingAgent teachingAgent;
    private final EvaluationAgent evaluationAgent;
    private final ResourceAgent resourceAgent;
    private final PresentationAgent presentationAgent;
    private final MasteryUpdateService masteryUpdateService;
    private final BilibiliVideoService bilibiliVideoService;
    private final StudentProfileRepository studentProfileRepository;
    private final ObjectMapper objectMapper;
    private final LlmService llmService;
    private final PptxRenderer pptxRenderer;
    private final AnimationAiService animationAiService;
    private static final String[] OPTION_KEY_FALLBACKS = {"A", "B", "C", "D", "E", "F"};
    private static final int LESSON_AGENT_TIMEOUT_SECONDS = 45;
    /**
     * 课程快照结构版本号。每当课程快照的数据结构或生成逻辑发生变化、导致旧快照不再符合
     * 预期时，将此值 +1：读取时版本不匹配的旧快照会被视为未命中并自动重新生成，
     * 无需手动清理数据库。
     */
    private static final int LESSON_SNAPSHOT_VERSION = 4;
    private static final String SNAPSHOT_VERSION_KEY = "snapshotVersion";
    private static final int SLIDES_SNAPSHOT_VERSION = 1;
    private static final String SLIDES_SNAPSHOT_VERSION_KEY = "slidesSnapshotVersion";

    /** SSE 讲解流的心跳调度器，每 15s 发注释行保活，防止代理超时断连。 */
    private final ScheduledExecutorService sseHeartbeatScheduler =
            Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r, "lesson-sse-heartbeat");
                t.setDaemon(true);
                return t;
            });

    public LearningService(LearningPathRepository learningPathRepository,
                           TracingResultRepository tracingResultRepository,
                           DiagnosticSessionRepository diagnosticSessionRepository,
                           KnowledgeGraphService knowledgeGraphService,
                           PlanningAgent planningAgent,
                           TeachingAgent teachingAgent,
                           EvaluationAgent evaluationAgent,
                           ResourceAgent resourceAgent,
                           PresentationAgent presentationAgent,
                           MasteryUpdateService masteryUpdateService,
                           BilibiliVideoService bilibiliVideoService,
                           StudentProfileRepository studentProfileRepository,
                           ObjectMapper objectMapper,
                           LlmService llmService,
                           PptxRenderer pptxRenderer,
                           AnimationAiService animationAiService) {
        this.learningPathRepository = learningPathRepository;
        this.tracingResultRepository = tracingResultRepository;
        this.diagnosticSessionRepository = diagnosticSessionRepository;
        this.knowledgeGraphService = knowledgeGraphService;
        this.planningAgent = planningAgent;
        this.teachingAgent = teachingAgent;
        this.evaluationAgent = evaluationAgent;
        this.resourceAgent = resourceAgent;
        this.presentationAgent = presentationAgent;
        this.masteryUpdateService = masteryUpdateService;
        this.bilibiliVideoService = bilibiliVideoService;
        this.studentProfileRepository = studentProfileRepository;
        this.objectMapper = objectMapper;
        this.llmService = llmService;
        this.pptxRenderer = pptxRenderer;
        this.animationAiService = animationAiService;
    }

    /**
     * 生成学习路径：调用 PlanningAgent + KnowledgeGraphService
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> generatePath(Long studentId, Object tracingResultRef,
                                            String targetKnowledgePointId, String mode,
                                            Integer dailyStudyMinutes) {
        TracingResult tracingResult = resolveTracingResult(tracingResultRef);
        Long tracingResultId = tracingResult.getId();
        if (!tracingResult.getStudentId().equals(studentId)) {
            throw new BusinessException(403, "无权使用该溯源结果生成学习路径");
        }

        if (targetKnowledgePointId == null || targetKnowledgePointId.isBlank() || "review".equals(targetKnowledgePointId)) {
            targetKnowledgePointId = inferTargetKnowledgePointId(tracingResult);
        }
        if (targetKnowledgePointId == null || targetKnowledgePointId.isBlank()) {
            throw new BusinessException(400, "目标知识点不能为空");
        }

        if (mode == null || mode.isEmpty()) {
            mode = "systematic";
        }
        // 画像驱动：日学习时长缺省时用画像里的设置，再缺省回退 30；基础水平用于按节奏缩放节点时长
        StudentProfile planProfile = studentProfileRepository.findByStudentId(studentId).orElse(null);
        if (dailyStudyMinutes == null && planProfile != null && planProfile.getDailyStudyMinutes() != null) {
            dailyStudyMinutes = planProfile.getDailyStudyMinutes();
        }
        if (dailyStudyMinutes == null) {
            dailyStudyMinutes = 30;
        }
        String planFoundation = planProfile != null ? planProfile.getFoundationLevel() : null;

        Optional<LearningPath> existingPath = learningPathRepository
                .findByStudentIdAndTargetKnowledgePointIdAndStatus(studentId, targetKnowledgePointId, "active");
        if (existingPath.isPresent()) {
            LearningPath existing = existingPath.get();
            if (isEmptyLearningPath(existing)) {
                log.warn("发现空学习路径，标记为 invalid 后重新生成: pathId={}, target={}",
                        existing.getId(), targetKnowledgePointId);
                existing.setStatus("invalid");
                learningPathRepository.save(existing);
            } else {
                throw new BusinessException(400, "该知识点已有进行中的学习路径");
            }
        }

        // 从溯源结果中提取根因信息
        List<Map<String, Object>> mergedRootCauses = parseJson(tracingResult.getMergedRootCauses(),
                new TypeReference<List<Map<String, Object>>>() {}, Collections.emptyList());

        List<String> rootCauseIds = mergedRootCauses.stream()
                .map(this::extractKnowledgePointId)
                .filter(id -> !id.isBlank())
                .distinct()
                .collect(Collectors.toList());
        Map<String, Object> tracingNameIndex = buildTracingPointNameIndex(tracingResult);

        List<String> weakPointIds = buildWeakPointsFromTracing(tracingResult).stream()
                .map(this::extractKnowledgePointId)
                .filter(id -> !id.isBlank())
                .distinct()
                .collect(Collectors.toList());

        // 用知识图谱获取目标节点的完整前置闭包，再叠加根因/薄弱点，避免漏掉中间必修节点。
        Set<String> pathNodeIds = buildPathNodeIdClosure(targetKnowledgePointId, rootCauseIds, weakPointIds);

        // 拓扑排序得到学习顺序
        List<KnowledgeNode> sortedNodes = knowledgeGraphService.topologicalSort(new ArrayList<>(pathNodeIds));
        boolean targetInKnowledgeGraph = knowledgeGraphService.getNode(targetKnowledgePointId) != null;
        boolean graphBackedPath = targetInKnowledgeGraph && !sortedNodes.isEmpty();
        String fallbackModuleName = resolveModuleFromTracing(tracingResult);

        // 构建 AgentContext 调用 PlanningAgent
        KnowledgeNode targetNode = targetInKnowledgeGraph ? knowledgeGraphService.getNode(targetKnowledgePointId) : null;
        String moduleName = targetNode != null ? targetNode.getModule() : fallbackModuleName;

        AgentContext context = AgentContext.builder()
                .studentId(studentId)
                .module(moduleName)
                .rootCauses(rootCauseIds)
                .knowledgeMastery(buildMasteryFromTracing(tracingResult))
                .weakPoints(buildWeakPointsFromTracing(tracingResult))
                .sessionData(new HashMap<>())
                .build();

        // 将拓扑序信息放入 sessionData
        List<String> topoOrder = sortedNodes.stream().map(KnowledgeNode::getId).collect(Collectors.toList());
        context.putSessionData("topologicalOrder", topoOrder);

        // 构建学习节点
        List<Map<String, Object>> nodes;
        if (graphBackedPath) {
            AgentResponse planResponse = planningAgent.execute(context);
            if (planResponse.isSuccess()) {
                nodes = buildNodesFromPlanningResult(planResponse.getData(), sortedNodes, dailyStudyMinutes);
                if (nodes.size() < sortedNodes.size() || !pathCoversTarget(nodes, targetKnowledgePointId)) {
                    log.warn("PlanningAgent 路径缺少必要节点，回退到知识图谱闭包路径: target={}, planned={}, expected={}",
                            targetKnowledgePointId, nodes.size(), sortedNodes.size());
                    nodes = buildNodesFromGraphFallback(sortedNodes, dailyStudyMinutes);
                }
            } else {
                log.warn("PlanningAgent 失败，使用知识图谱拓扑序作为 fallback: {}", planResponse.getMessage());
                nodes = buildNodesFromGraphFallback(sortedNodes, dailyStudyMinutes);
            }
        } else {
            nodes = buildNodesFromTracingFallback(tracingResult, targetKnowledgePointId, rootCauseIds,
                    tracingNameIndex, fallbackModuleName, dailyStudyMinutes);
        }

        if (nodes.isEmpty()) {
            nodes = graphBackedPath
                    ? buildNodesFromGraphFallback(sortedNodes, dailyStudyMinutes)
                    : buildNodesFromTracingFallback(tracingResult, targetKnowledgePointId, rootCauseIds,
                            tracingNameIndex, fallbackModuleName, dailyStudyMinutes);
        }
        if (nodes.isEmpty()) {
            throw new BusinessException(500, "未能生成有效学习路径节点，请重新进行溯因分析");
        }

        // 画像驱动：按基础水平缩放每个节点的预计时长（基础弱→更充裕，较强→更紧凑）
        applyPaceToNodes(nodes, paceFactorByFoundation(planFoundation));

        int totalEstimated = nodes.stream()
                .mapToInt(n -> ((Number) n.getOrDefault("estimatedMinutes", 20)).intValue())
                .sum();

        // 确定根因节点
        String rootCauseId = !rootCauseIds.isEmpty() ? rootCauseIds.get(0) : (sortedNodes.isEmpty() ? targetKnowledgePointId : sortedNodes.get(0).getId());
        String rootCauseName = resolveKpName(rootCauseId, tracingNameIndex);
        String targetName = resolveKpName(targetKnowledgePointId, tracingNameIndex);

        LearningPath path = new LearningPath();
        path.setStudentId(studentId);
        path.setTracingResultId(tracingResultId);
        path.setTargetKnowledgePointId(targetKnowledgePointId);
        path.setTargetKnowledgePointName(targetName);
        path.setRootCausePointId(rootCauseId);
        path.setRootCausePointName(rootCauseName);
        path.setMode(mode);
        path.setStatus("active");
        path.setProgress(BigDecimal.ZERO);
        path.setTotalEstimatedMinutes(totalEstimated);
        path.setTotalNodes(nodes.size());
        path.setCompletedNodes(0);
        if (!nodes.isEmpty()) {
            path.setCurrentNodeId(String.valueOf(nodes.get(0).get("nodeId")));
        }

        try {
            path.setNodes(objectMapper.writeValueAsString(nodes));
            List<Map<String, Object>> tracingPath = buildTracingPathEdges(nodes);
            path.setTracingPath(objectMapper.writeValueAsString(tracingPath));
        } catch (Exception e) {
            throw new BusinessException(500, "序列化节点数据失败");
        }

        path = learningPathRepository.save(path);
        return buildPathResponse(path, nodes);
    }

    private boolean isEmptyLearningPath(LearningPath path) {
        if (path.getTotalNodes() != null && path.getTotalNodes() > 0) {
            return false;
        }
        List<Map<String, Object>> nodes = parseJson(path.getNodes(),
                new TypeReference<List<Map<String, Object>>>() {}, Collections.emptyList());
        return nodes.isEmpty();
    }

    private TracingResult resolveTracingResult(Object tracingResultRef) {
        if (tracingResultRef == null || tracingResultRef.toString().trim().isEmpty()
                || "undefined".equals(tracingResultRef.toString())) {
            throw new BusinessException(400, "tracingResultId不能为空");
        }

        String ref = tracingResultRef.toString().trim();
        if (tracingResultRef instanceof Number || ref.matches("\\d+")) {
            return tracingResultRepository.findById(Long.valueOf(ref))
                    .orElseThrow(() -> new BusinessException(404, "溯源结果不存在"));
        }

        return tracingResultRepository.findByTracingId(ref)
                .orElseThrow(() -> new BusinessException(404, "溯源结果不存在"));
    }

    private Set<String> buildPathNodeIdClosure(String targetKnowledgePointId,
                                               List<String> rootCauseIds,
                                               List<String> weakPointIds) {
        Set<String> ids = new LinkedHashSet<>();
        addKnowledgePointWithPrerequisites(ids, targetKnowledgePointId);
        for (String rootId : rootCauseIds) {
            addKnowledgePointWithPrerequisites(ids, rootId);
        }
        for (String weakId : weakPointIds) {
            addKnowledgePointWithPrerequisites(ids, weakId);
        }
        ids.remove("review");
        ids.remove("unknown");
        return ids;
    }

    private void addKnowledgePointWithPrerequisites(Set<String> ids, String rawId) {
        String id = firstNonBlank(rawId);
        if (id.isBlank() || "review".equals(id) || "unknown".equals(id)) {
            return;
        }
        for (KnowledgeNode prereq : knowledgeGraphService.getAllPrerequisitesRecursive(id)) {
            ids.add(prereq.getId());
        }
        ids.add(id);
    }

    private String extractKnowledgePointId(Map<String, Object> data) {
        if (data == null) return "";
        return firstNonBlank(
                data.get("knowledgePointId"),
                data.get("kpId"),
                data.get("targetPointId"),
                data.get("nodeId"),
                data.get("id"));
    }

    private boolean pathCoversTarget(List<Map<String, Object>> nodes, String targetKnowledgePointId) {
        if (targetKnowledgePointId == null || targetKnowledgePointId.isBlank()) {
            return true;
        }
        for (Map<String, Object> node : nodes) {
            if (targetKnowledgePointId.equals(firstNonBlank(node.get("knowledgePointId")))) {
                return true;
            }
        }
        return false;
    }

    private String inferTargetKnowledgePointId(TracingResult tracingResult) {
        String fallbackId = null;

        List<Map<String, Object>> tracingResults = parseJson(tracingResult.getTracingResults(),
                new TypeReference<List<Map<String, Object>>>() {}, Collections.emptyList());
        for (Map<String, Object> tr : tracingResults) {
            String targetId = firstNonBlank(tr.get("targetPointId"));
            if (!targetId.isBlank()) {
                if (knowledgeGraphService.getNode(targetId) != null) {
                    return targetId;
                }
                if (fallbackId == null) {
                    fallbackId = targetId;
                }
            }
        }

        List<Map<String, Object>> suggestedPath = parseJson(tracingResult.getSuggestedLearningPath(),
                new TypeReference<List<Map<String, Object>>>() {}, Collections.emptyList());
        for (int i = suggestedPath.size() - 1; i >= 0; i--) {
            String kpId = firstNonBlank(suggestedPath.get(i).get("knowledgePointId"));
            if (!kpId.isBlank() && !"review".equals(kpId)) {
                if (knowledgeGraphService.getNode(kpId) != null) {
                    return kpId;
                }
                if (fallbackId == null) {
                    fallbackId = kpId;
                }
            }
        }
        return fallbackId;
    }

    /**
     * 构建学生画像信息（专业/学历/课程/目标/推荐资源类型），用于路径页展示
     */
    private Map<String, Object> buildProfileInfo(Long studentId) {
        Map<String, Object> info = new LinkedHashMap<>();
        StudentProfile profile = studentProfileRepository.findByStudentId(studentId).orElse(null);
        if (profile != null) {
            info.put("majorDirection", profile.getMajorDirection());
            info.put("educationLevel", profile.getEducationLevel());
            info.put("currentCourse", profile.getCurrentCourse());
            info.put("learningGoal", profile.getLearningGoal());
            info.put("foundationLevel", profile.getFoundationLevel());
        }
        info.put("recommendedResourceTypes", resolveResourceTypes(profile));
        return info;
    }

    @SuppressWarnings("unchecked")
    private List<String> resolveResourceTypes(StudentProfile profile) {
        List<String> defaults = Arrays.asList(
                "讲解文档", "思维导图", "分层练习", "拓展阅读", "实操案例", "视频推荐", "动画脚本");
        if (profile == null || profile.getResourcePreference() == null
                || profile.getResourcePreference().isBlank()) {
            return defaults;
        }
        try {
            List<String> prefs = objectMapper.readValue(profile.getResourcePreference(),
                    new TypeReference<List<String>>() {});
            return (prefs == null || prefs.isEmpty()) ? defaults : prefs;
        } catch (Exception e) {
            return defaults;
        }
    }

    /**
     * 获取学习路径列表
     */
    public Map<String, Object> getPathList(Long studentId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LearningPath> pathPage;

        if (status != null && !status.isEmpty()) {
            pathPage = learningPathRepository.findByStudentIdAndStatusOrderByCreatedAtDesc(studentId, status, pageable);
        } else {
            pathPage = learningPathRepository.findByStudentIdOrderByCreatedAtDesc(studentId, pageable);
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (LearningPath p : pathPage.getContent()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("pathId", p.getId());
            item.put("title", buildPathTitle(p));
            List<Map<String, Object>> nodes = parseJson(p.getNodes(),
                    new TypeReference<List<Map<String, Object>>>() {}, Collections.emptyList());
            String moduleName = resolvePathModule(p, nodes);
            item.put("module", moduleName);

            Map<String, Object> targetKp = new LinkedHashMap<>();
            targetKp.put("id", p.getTargetKnowledgePointId());
            targetKp.put("name", p.getTargetKnowledgePointName());
            targetKp.put("module", moduleName);
            item.put("targetKnowledgePoint", targetKp);

            Map<String, Object> rootCauseKp = new LinkedHashMap<>();
            rootCauseKp.put("id", p.getRootCausePointId());
            rootCauseKp.put("name", p.getRootCausePointName());
            item.put("rootCausePoint", rootCauseKp);

            Map<String, Object> currentNode = new LinkedHashMap<>();
            currentNode.put("nodeId", p.getCurrentNodeId());
            currentNode.put("knowledgePointName", p.getCurrentNodeId() != null
                    ? resolveKpNameFromNodes(nodes, p.getCurrentNodeId()) : null);
            item.put("currentNode", p.getCurrentNodeId() != null ? currentNode : null);

            item.put("totalNodes", p.getTotalNodes());
            item.put("completedNodes", p.getCompletedNodes());
            item.put("progress", toProgressPercent(p.getProgress()));
            item.put("status", p.getStatus());
            item.put("mode", p.getMode());
            item.put("totalEstimatedMinutes", p.getTotalEstimatedMinutes());
            item.put("actualStudyMinutes", p.getActualStudyMinutes());
            item.put("createdAt", p.getCreatedAt());
            item.put("lastStudyAt", p.getLastStudyAt());
            items.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paths", items);
        result.put("profile", buildProfileInfo(studentId));
        result.put("total", pathPage.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", pathPage.getTotalPages());
        return result;
    }

    /**
     * 获取路径详情
     */
    public Map<String, Object> getPathDetail(Long studentId, Long pathId) {
        LearningPath path = learningPathRepository.findById(pathId)
                .orElseThrow(() -> new BusinessException(404, "学习路径不存在"));

        if (!path.getStudentId().equals(studentId)) {
            throw new BusinessException(403, "无权访问该学习路径");
        }

        List<Map<String, Object>> nodes = parseJson(path.getNodes(),
                new TypeReference<List<Map<String, Object>>>() {}, new ArrayList<>());

        return buildPathResponse(path, nodes);
    }

    /**
     * 生成个性化教学内容：调用 TeachingAgent
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> generateLesson(Long studentId, Long pathId, String nodeId) {
        LearningPath path = learningPathRepository.findById(pathId)
                .orElseThrow(() -> new BusinessException(404, "学习路径不存在"));

        if (!path.getStudentId().equals(studentId)) {
            throw new BusinessException(403, "无权访问该学习路径");
        }

        List<Map<String, Object>> nodes = parseJson(path.getNodes(),
                new TypeReference<List<Map<String, Object>>>() {}, Collections.emptyList());

        Map<String, Object> targetNode = nodes.stream()
                .filter(n -> nodeId.equals(n.get("nodeId")))
                .findFirst()
                .orElseThrow(() -> new BusinessException(404, "节点不存在"));

        String nodeStatus = String.valueOf(targetNode.getOrDefault("status", "unlocked"));
        if ("locked".equals(nodeStatus)) {
            throw new BusinessException(400, "该节点尚未解锁，请先完成前置节点");
        }
        if (!"completed".equals(nodeStatus)) {
            targetNode.put("status", "in_progress");
            path.setCurrentNodeId(nodeId);
            try {
                path.setNodes(objectMapper.writeValueAsString(nodes));
                path.setLastStudyAt(LocalDateTime.now());
                learningPathRepository.save(path);
            } catch (Exception e) {
                throw new BusinessException(500, "更新节点状态失败");
            }
        }

        Map<String, Object> cachedLesson = findLessonSnapshotResponse(path, nodeId);
        if (!cachedLesson.isEmpty()) {
            cachedLesson.put("pathId", pathId);
            cachedLesson.put("nodeId", nodeId);
            cachedLesson.put("nodeStatus", targetNode.get("status"));
            path.setLastStudyAt(LocalDateTime.now());
            learningPathRepository.save(path);
            return cachedLesson;
        }

        String kpId = (String) targetNode.getOrDefault("knowledgePointId", nodeId);
        String kpName = (String) targetNode.getOrDefault("knowledgePointName", resolveKpName(kpId));
        double currentMastery = 0.5;
        Object masteryObj = targetNode.get("currentMastery");
        if (masteryObj instanceof Number) {
            currentMastery = ((Number) masteryObj).doubleValue();
        }

        // 从知识图谱获取知识点详情
        KnowledgeNode kpNode = knowledgeGraphService.getNode(kpId);
        String kpDesc = kpNode != null ? kpNode.getDescription() : "";
        String moduleName = resolveNodeModule(targetNode, kpNode);
        List<String> errorTypes = kpNode != null ? collectCommonErrors(kpNode) : Collections.emptyList();

        // 多智能体协作生成：教学Agent 产出练习块，资源Agent 产出多模态资源，二者并行。
        // 讲解块另由流式接口（streamLessonExplanation）产出，三路分工互不阻塞。
        AgentContext exerciseContext = buildTeachingContext(studentId, moduleName, kpId, kpName,
                kpDesc, currentMastery, errorTypes, TeachingAgent.SCOPE_EXERCISE);
        AgentContext resourceContext = buildTeachingContext(studentId, moduleName, kpId, kpName,
                kpDesc, currentMastery, errorTypes, "resource");

        CompletableFuture<AgentResponse> exerciseFuture =
                CompletableFuture.supplyAsync(() -> teachingAgent.execute(exerciseContext));
        CompletableFuture<AgentResponse> resourceFuture =
                CompletableFuture.supplyAsync(() -> resourceAgent.execute(resourceContext));

        AgentResponse teachResponse = buildExerciseResponse(
                awaitAgent(exerciseFuture, kpName, "练习"), kpName);
        AgentResponse resourceResponse = awaitAgent(resourceFuture, kpName, "多模态资源");
        Map<String, Object> resourceAgentData =
                (resourceResponse != null && resourceResponse.isSuccess() && resourceResponse.getData() != null)
                        ? resourceResponse.getData() : Collections.emptyMap();
        log.info("学习节点资源协作: 教学Agent(练习)+资源Agent(多模态) 完成, kpName={}, 资源Agent有效={}",
                kpName, !resourceAgentData.isEmpty());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pathId", pathId);
        result.put("nodeId", nodeId);

        Map<String, Object> knowledgePoint = new LinkedHashMap<>();
        knowledgePoint.put("id", kpId);
        knowledgePoint.put("name", kpName);
        knowledgePoint.put("module", moduleName);
        knowledgePoint.put("difficulty", kpNode != null ? kpNode.getDifficulty() : 3);
        result.put("knowledgePoint", knowledgePoint);

        result.put("studentMastery", currentMastery);
        result.put("nodeStatus", targetNode.get("status"));
        result.put("checkpointThreshold", 0.6);

        // 确定教学策略标签
        String strategy;
        String strategyLabel;
        if (currentMastery < 0.4) {
            strategy = "basic_consolidation";
            strategyLabel = "基础巩固";
        } else if (currentMastery < 0.7) {
            strategy = "strengthening";
            strategyLabel = "强化训练";
        } else {
            strategy = "expansion";
            strategyLabel = "拓展提高";
        }
        result.put("teachingStrategy", strategy);
        result.put("teachingStrategyLabel", strategyLabel);
        // 画像驱动：输出学生实际学习风格（驱动讲解呈现方式），缺失时回退 adaptive
        Object styleVal = exerciseContext.getStudentProfile() != null
                ? exerciseContext.getStudentProfile().get("learningStyle") : null;
        String cognitiveStyle = (styleVal != null && !String.valueOf(styleVal).isBlank()
                && !"null".equals(String.valueOf(styleVal))) ? String.valueOf(styleVal) : "adaptive";
        result.put("cognitiveStyle", cognitiveStyle);

        Map<String, Object> lesson = new LinkedHashMap<>();
        boolean lessonComplete;
        if (teachResponse.isSuccess()) {
            Map<String, Object> lessonData = teachResponse.getData();
            lesson.put("title", lessonData.getOrDefault("title", kpName + " — " + strategyLabel));

            // 构建 sections
            List<Map<String, Object>> sections = new ArrayList<>();

            // 概念讲解：JSON 接口不再生成讲解，先放快速讲解占位，由流式接口随后覆盖为完整讲解
            Object conceptObj = lessonData.get("conceptExplanation");
            String conceptContent = "";
            Map<String, Object> conceptSection = new LinkedHashMap<>();
            conceptSection.put("type", "concept");
            conceptSection.put("title", "核心概念");
            if (conceptObj instanceof Map) {
                Map<String, Object> concept = (Map<String, Object>) conceptObj;
                conceptContent = String.valueOf(concept.getOrDefault("content", "")).trim();
                conceptSection.put("content", conceptContent);
                conceptSection.put("summary", concept.getOrDefault("summary", ""));
                conceptSection.put("keyFormulas", concept.getOrDefault("keyFormulas", Collections.emptyList()));
                conceptSection.put("visualDescription", concept.getOrDefault("visualDescription", ""));
            }
            boolean conceptFromAi = !conceptContent.isBlank();
            if (!conceptFromAi) {
                // 占位：标记 placeholder，待流式讲解完成后由 mergeStreamedExplanationToSnapshot 覆盖
                conceptSection.put("content", buildQuickConcept(kpName, kpDesc));
                conceptSection.put("summary", kpName + "的基础讲解");
                conceptSection.put("keyFormulas", buildQuickFormulas(kpName));
                conceptSection.put("placeholder", true);
            }
            sections.add(conceptSection);

            // 例题：AI 缺失时用快速例题兜底
            Object examplesObj = lessonData.get("examples");
            boolean examplesFromAi = examplesObj instanceof List && !((List<?>) examplesObj).isEmpty();
            Map<String, Object> exampleSection = new LinkedHashMap<>();
            exampleSection.put("type", "example");
            exampleSection.put("title", "典型例题");
            exampleSection.put("content", examplesFromAi ? examplesObj : buildQuickExamples(kpName));
            sections.add(exampleSection);

            // 常见错误
            Object mistakesObj = lessonData.get("commonMistakes");
            if (mistakesObj instanceof List && !((List<?>) mistakesObj).isEmpty()) {
                Map<String, Object> mistakeSection = new LinkedHashMap<>();
                mistakeSection.put("type", "key_points");
                mistakeSection.put("title", "常见错误与纠正");
                mistakeSection.put("content", formatLessonObjectList((List<?>) mistakesObj));
                sections.add(mistakeSection);
            }

            lesson.put("sections", sections);

            // 练习题：AI 缺失时用快速练习兜底，保证练习/检查点可用
            Object exercisesObj = lessonData.get("exercises");
            boolean exercisesFromAi = exercisesObj instanceof List && !((List<?>) exercisesObj).isEmpty();
            List<Map<String, Object>> exercises = new ArrayList<>();
            if (exercisesFromAi) {
                for (Object exerciseObj : (List<?>) exercisesObj) {
                    if (!(exerciseObj instanceof Map)) continue;
                    Map<String, Object> ex = new LinkedHashMap<>((Map<String, Object>) exerciseObj);
                    exercises.add(normalizeLessonExercise(ex, exercises.size(), nodeId));
                }
            }
            if (exercises.isEmpty()) {
                exercises.addAll(buildQuickExercises(kpName, nodeId));
            }
            lesson.put("exercises", exercises);
            lesson.put("totalExercises", exercises.size());

            lesson.put("summary", lessonData.getOrDefault("summary", ""));
            result.put("lesson", lesson);

            // 讲解走流式，这里只要练习真实生成即视为可缓存；讲解由流式接口回填覆盖占位
            lessonComplete = exercisesFromAi;
        } else {
            log.warn("TeachingAgent 失败: {}", teachResponse.getMessage());
            lesson.putAll(buildQuickLesson(kpName, kpDesc, strategyLabel, errorTypes, nodeId));
            result.put("lesson", lesson);
            lessonComplete = false;
        }

        String generatedAt = LocalDateTime.now().toString();
        result.put("generatedAt", generatedAt);
        result.put("degraded", !lessonComplete);
        // 资源Agent 原始产出存入快照，供资源卡片详情弹窗复用，避免详情再次调模型
        if (!resourceAgentData.isEmpty()) {
            result.put("resourceAgentData", resourceAgentData);
        }
        enrichLessonResponseForFrontend(result, lesson, kpName);
        result.put("resources", buildLessonResources(lesson, kpNode, kpName, moduleName, resourceAgentData,
                exerciseContext.getStudentProfile()));
        result.put("assistantPrompts", buildAssistantPrompts(kpName, strategyLabel));
        // 内容完整才落快照；降级内容只本次返回，避免半成品被缓存后“重开依旧失败”
        if (lessonComplete) {
            saveLessonSnapshot(path, nodeId, result);
        } else {
            log.warn("课程内容降级（讲解或练习未完整生成），跳过快照缓存: pathId={}, nodeId={}", pathId, nodeId);
        }
        return result;
    }

    /**
     * 以 SSE 流式输出该节点的「讲解正文」。
     * <p>
     * 长篇讲解生成耗时常超过同步超时阈值（实测 >45s），同步生成会被掐断并降级。
     * 改为流式后，token 边生成边推送，用户 2-3 秒即可看到讲解开始，且不再受 45s 限制。
     * 练习题、例题、资源卡片等结构化数据仍由 {@link #generateLesson} 的 JSON 接口负责。
     * <p>
     * 已有完整快照时直接回放快照中的讲解（秒回，不重复生成）；否则现场流式生成，
     * 完成后把讲解正文回填到课程快照，供资源详情等后续功能复用。
     */
    @SuppressWarnings("unchecked")
    public SseEmitter streamLessonExplanation(Long studentId, Long pathId, String nodeId) {
        SseEmitter emitter = new SseEmitter(180_000L);
        AtomicBoolean alive = new AtomicBoolean(true);

        ScheduledFuture<?> heartbeat = sseHeartbeatScheduler.scheduleAtFixedRate(() -> {
            if (!alive.get()) return;
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException e) {
                alive.set(false);
            }
        }, 15, 15, TimeUnit.SECONDS);
        emitter.onCompletion(() -> { alive.set(false); heartbeat.cancel(false); });
        emitter.onTimeout(() -> { alive.set(false); heartbeat.cancel(false); emitter.complete(); });
        emitter.onError(t -> { alive.set(false); heartbeat.cancel(false); });

        LearningPath path;
        Map<String, Object> targetNode;
        try {
            path = learningPathRepository.findById(pathId)
                    .orElseThrow(() -> new BusinessException(404, "学习路径不存在"));
            if (!path.getStudentId().equals(studentId)) {
                throw new BusinessException(403, "无权访问该学习路径");
            }
            List<Map<String, Object>> nodes = parseJson(path.getNodes(),
                    new TypeReference<List<Map<String, Object>>>() {}, Collections.emptyList());
            targetNode = nodes.stream()
                    .filter(n -> nodeId.equals(n.get("nodeId")))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(404, "节点不存在"));
        } catch (Exception e) {
            sendSseError(emitter, alive, heartbeat, e.getMessage());
            return emitter;
        }

        // 1. 命中完整快照且讲解非占位：直接回放已有讲解，秒回
        Map<String, Object> snapshot = findLessonSnapshotResponse(path, nodeId);
        if (!snapshot.isEmpty()) {
            Map<String, Object> lesson = snapshot.get("lesson") instanceof Map
                    ? (Map<String, Object>) snapshot.get("lesson") : Collections.emptyMap();
            Map<String, Object> concept = extractConceptSection(lesson);
            boolean placeholder = Boolean.TRUE.equals(concept.get("placeholder"));
            String cachedContent = String.valueOf(concept.getOrDefault("content", "")).trim();
            if (!placeholder && !cachedContent.isBlank() && !"null".equals(cachedContent)) {
                try {
                    emitter.send(SseEmitter.event().name("message").data(Map.of("content", cachedContent)));
                    emitter.send(SseEmitter.event().name("done").data(Map.of("cached", true)));
                } catch (IOException ignored) {
                } finally {
                    alive.set(false);
                    heartbeat.cancel(false);
                    emitter.complete();
                }
                return emitter;
            }
        }

        // 2. 现场流式生成
        String kpId = (String) targetNode.getOrDefault("knowledgePointId", nodeId);
        String kpName = (String) targetNode.getOrDefault("knowledgePointName", resolveKpName(kpId));
        double currentMastery = 0.5;
        Object masteryObj = targetNode.get("currentMastery");
        if (masteryObj instanceof Number) {
            currentMastery = ((Number) masteryObj).doubleValue();
        }
        KnowledgeNode kpNode = knowledgeGraphService.getNode(kpId);
        String kpDesc = kpNode != null ? kpNode.getDescription() : "";
        String moduleName = resolveNodeModule(targetNode, kpNode);

        Map<String, Object> profileMap = loadStudentProfileMap(studentId);
        putIfPresent(profileMap, "currentCourse", moduleName);
        List<Map<String, String>> messages = buildExplainStreamMessages(kpName, kpDesc, currentMastery, profileMap);
        StringBuilder full = new StringBuilder();
        final LearningPath fPath = path;

        try {
            emitter.send(SseEmitter.event().name("metadata").data(
                    Map.of("knowledgePointName", kpName, "nodeId", nodeId)));
        } catch (IOException ignored) {
        }

        llmService.streamChat(messages, 0.7,
                token -> {
                    if (!alive.get()) return;
                    full.append(token);
                    try {
                        emitter.send(SseEmitter.event().name("message").data(Map.of("content", token)));
                    } catch (IOException e) {
                        alive.set(false);
                    }
                },
                () -> {
                    // 流式完成：把讲解正文回填到课程快照，供资源详情等复用
                    try {
                        mergeStreamedExplanationToSnapshot(fPath, nodeId, kpName, full.toString());
                    } catch (Exception e) {
                        log.warn("回填讲解快照失败: nodeId={}, {}", nodeId, e.getMessage());
                    }
                    if (alive.get()) {
                        try {
                            emitter.send(SseEmitter.event().name("done").data(Map.of("cached", false)));
                            emitter.complete();
                        } catch (IOException e) {
                            emitter.complete();
                        }
                    }
                },
                error -> {
                    log.warn("讲解流式生成失败: nodeId={}, {}", nodeId, error.getMessage());
                    sendSseError(emitter, alive, heartbeat, "讲解生成失败，请重试");
                });

        return emitter;
    }

    private void sendSseError(SseEmitter emitter, AtomicBoolean alive,
                              ScheduledFuture<?> heartbeat, String message) {
        if (alive.get()) {
            try {
                emitter.send(SseEmitter.event().name("error")
                        .data(Map.of("error", message == null ? "讲解生成失败" : message)));
            } catch (IOException ignored) {
            }
        }
        alive.set(false);
        heartbeat.cancel(false);
        emitter.complete();
    }

    /** 构造讲解流式生成的对话消息：输出 Markdown 正文（非 JSON），更适合逐字流式展示。 */
    private List<Map<String, String>> buildExplainStreamMessages(String kpName, String kpDesc, double mastery,
                                                                 Map<String, Object> profile) {
        String depthHint;
        if (mastery < 0.4) {
            depthHint = "学生基础较弱，用直观生活实例引入，多打比方，循序渐进。";
        } else if (mastery < 0.7) {
            depthHint = "学生有一定基础，聚焦易混淆点与常见错误，配合变式讲清楚。";
        } else {
            depthHint = "学生掌握度较高，可适当深入原理、迁移应用与综合分析。";
        }

        String sys = "你是 SmartMentor 智学导师，负责把一个高校课程知识点讲到学生“仅凭这份讲解就能学懂”的程度。"
                + "直接输出 Markdown 正文，不要使用 JSON、不要用代码块包裹整篇、不要写多余的开场白或结束语。"
                + "讲解必须分段展开、内容详实（不少于 400 字），依次覆盖：是什么、为什么/原理、适用条件与边界、"
                + "怎么用/步骤、典型例子、易错点。公式用 $...$ 或 $$...$$ 包裹，禁止裸 LaTeX，禁止用 ASCII 字符画绘制图表。"
                + "请结合学生的专业方向与课程背景举例，让讲解贴合其专业应用场景。"
                + depthHint;

        StringBuilder user = new StringBuilder();
        user.append("请讲解知识点：").append(kpName).append("\n");
        if (kpDesc != null && !kpDesc.isBlank()) {
            user.append("知识点说明：").append(kpDesc).append("\n");
        }
        if (profile != null && !profile.isEmpty()) {
            user.append("\n【学生画像，讲解须据此个性化】\n");
            appendUserProfile(user, profile, "majorDirection", "专业方向");
            appendUserProfile(user, profile, "currentCourse", "当前课程");
            appendUserProfile(user, profile, "educationLevel", "学历层次");
            appendUserProfile(user, profile, "learningGoal", "学习目标");
            appendUserProfile(user, profile, "foundationLevel", "基础水平");
            appendUserProfile(user, profile, "weakModulePriority", "薄弱模块");
        }
        user.append("\n请现在开始讲解。");

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", sys));
        messages.add(Map.of("role", "user", "content", user.toString()));
        return messages;
    }

    private void appendUserProfile(StringBuilder sb, Map<String, Object> profile, String key, String label) {
        Object v = profile.get(key);
        if (v != null && !String.valueOf(v).isBlank() && !"null".equals(String.valueOf(v))) {
            sb.append("- ").append(label).append("：").append(v).append("\n");
        }
    }

    /**
     * 把流式生成的讲解正文回填进课程快照的 concept section。
     * 若该节点尚无快照，则不创建（练习等结构化数据由 JSON 接口生成时再落快照），
     * 仅在已有快照时补全/更新讲解内容，保证资源详情等功能有素材可用。
     */
    @SuppressWarnings("unchecked")
    private synchronized void mergeStreamedExplanationToSnapshot(LearningPath path, String nodeId,
                                                                 String kpName, String explanation) {
        if (explanation == null || explanation.isBlank()) {
            return;
        }
        LearningPath fresh = learningPathRepository.findById(path.getId()).orElse(path);
        Map<String, Object> snapshots = new LinkedHashMap<>(parseJson(fresh.getLessonSnapshots(),
                new TypeReference<Map<String, Object>>() {}, Collections.emptyMap()));
        Object snapObj = snapshots.get(nodeId);
        if (!(snapObj instanceof Map)) {
            // 无快照可补；不单独为讲解建快照，避免与 JSON 接口的完整性约束冲突
            return;
        }
        Map<String, Object> snapshot = new LinkedHashMap<>((Map<String, Object>) snapObj);
        Object lessonObj = snapshot.get("lesson");
        if (!(lessonObj instanceof Map)) {
            return;
        }
        Map<String, Object> lesson = new LinkedHashMap<>((Map<String, Object>) lessonObj);

        List<Map<String, Object>> sections = lesson.get("sections") instanceof List
                ? new ArrayList<>((List<Map<String, Object>>) lesson.get("sections"))
                : new ArrayList<>();
        boolean updated = false;
        for (Map<String, Object> section : sections) {
            if ("concept".equals(String.valueOf(section.get("type")))) {
                section.put("content", explanation);
                section.remove("placeholder");
                updated = true;
                break;
            }
        }
        if (!updated) {
            Map<String, Object> conceptSection = new LinkedHashMap<>();
            conceptSection.put("type", "concept");
            conceptSection.put("title", "核心概念");
            conceptSection.put("content", explanation);
            sections.add(0, conceptSection);
        }
        lesson.put("sections", sections);
        snapshot.put("lesson", lesson);
        // 同步刷新前端展示用的 content.sections
        enrichLessonResponseForFrontend(snapshot, lesson, kpName);
        snapshots.put(nodeId, snapshot);
        try {
            fresh.setLessonSnapshots(objectMapper.writeValueAsString(snapshots));
            learningPathRepository.save(fresh);
        } catch (Exception e) {
            log.warn("保存讲解快照失败: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildLessonResources(Map<String, Object> lesson,
                                                     KnowledgeNode kpNode,
                                                     String kpName,
                                                     String moduleName,
                                                     Map<String, Object> resourceAgentData,
                                                     Map<String, Object> profile) {
        // 画像字段（用于 fallback 模板个性化），缺失时为空串
        String pMajor = profile != null ? stringOrEmpty(profile.get("majorDirection")) : "";
        String pInterest = profile != null ? stringOrEmpty(profile.get("academicInterest")) : "";
        String pCourse = profile != null ? stringOrEmpty(profile.get("currentCourse")) : "";
        Map<String, Object> resources = new LinkedHashMap<>();

        // 1. 讲解文档：卡面只放“摘要”，完整讲解全文留给详情弹窗，避免卡面比详情还长
        Map<String, Object> conceptSection = extractConceptSection(lesson);
        String conceptSummary = String.valueOf(conceptSection.getOrDefault("summary", "")).trim();
        String conceptContent = extractConceptContent(lesson);
        String documentSummary;
        if (!conceptSummary.isBlank() && !"null".equals(conceptSummary)) {
            documentSummary = conceptSummary;
        } else if (!conceptContent.isBlank()) {
            documentSummary = summarize(conceptContent, 80);
        } else if (kpNode != null && kpNode.getDescription() != null && !kpNode.getDescription().isBlank()) {
            documentSummary = kpNode.getDescription();
        } else {
            documentSummary = "围绕「" + kpName + "」梳理核心定义、适用条件、典型方法与易错点。";
        }
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("title", kpName + " 讲解文档");
        document.put("content", documentSummary);
        resources.put("document", document);

        // 2. 思维导图：优先用资源Agent 个性化产出，失败回退知识图谱+模板
        Map<String, Object> agentMindMap = ResourceAgent.extractResource(resourceAgentData, "mindMap");
        Map<String, Object> mindMap = new LinkedHashMap<>();
        if (!agentMindMap.isEmpty()) {
            mindMap.put("title", agentMindMap.getOrDefault("title", kpName + " 知识结构思维导图"));
            mindMap.put("summary", agentMindMap.getOrDefault("summary", ""));
            mindMap.put("nodes", flattenMindMapBranches(agentMindMap, kpName));
            mindMap.put("aiGenerated", true);
        } else {
            List<String> mindNodes = new ArrayList<>();
            mindNodes.add("核心知识点：" + kpName);
            if (kpNode != null) {
                List<KnowledgeNode> prereqs = knowledgeGraphService.getPrerequisites(kpNode.getId());
                if (!prereqs.isEmpty()) {
                    mindNodes.add("前置基础：" + prereqs.stream()
                            .map(KnowledgeNode::getName)
                            .collect(Collectors.joining("、")));
                }
                if (kpNode.getCommonErrors() != null && !kpNode.getCommonErrors().isEmpty()) {
                    mindNodes.add("常见误区：" + String.join("、", kpNode.getCommonErrors()));
                }
            }
            mindNodes.add("应用方法：结合" + (isBlankText(moduleName) ? "课程" : moduleName) + "中的真实场景进行迁移");
            if (!pInterest.isEmpty()) {
                mindNodes.add("结合你的兴趣方向：从「" + pInterest + "」角度拓展理解");
            }
            mindMap.put("title", kpName + " 知识结构思维导图");
            mindMap.put("nodes", mindNodes);
        }
        resources.put("mindMap", mindMap);

        // 3. 分层练习：引用已生成的练习
        Object exercisesObj = lesson.get("exercises");
        int exerciseCount = exercisesObj instanceof List ? ((List<?>) exercisesObj).size() : 0;
        Map<String, Object> exercises = new LinkedHashMap<>();
        exercises.put("title", exerciseCount > 0 ? exerciseCount + " 道分层练习" : "分层练习");
        exercises.put("content", exerciseCount > 0
                ? "系统已按基础、强化、拓展三个层次准备练习题，可在下方练习区完成并获得即时反馈。"
                : "系统将根据你的掌握度动态生成分层练习。");
        resources.put("exercises", exercises);

        // 4. 拓展阅读：优先用资源Agent 个性化产出（贴合专业方向），失败回退模板
        Map<String, Object> agentReading = ResourceAgent.extractResource(resourceAgentData, "extendedReading");
        Map<String, Object> extendedReading = new LinkedHashMap<>();
        if (!agentReading.isEmpty()) {
            extendedReading.put("title", agentReading.getOrDefault("title", kpName + " 拓展阅读建议"));
            extendedReading.put("summary", agentReading.getOrDefault("summary", ""));
            extendedReading.put("items", flattenReadingItems(agentReading, kpName, moduleName));
            extendedReading.put("aiGenerated", true);
        } else {
            List<String> reading = new ArrayList<>();
            reading.add("课程背景：理解「" + kpName + "」在" + (isBlankText(moduleName) ? "本课程" : moduleName) + "中的定位与意义。");
            reading.add("工程应用：查阅「" + kpName + "」在"
                    + (pMajor.isEmpty() ? "真实项目或行业" : "『" + pMajor + "』方向的真实项目")
                    + "中的应用案例。");
            reading.add("前沿进展：关注「" + kpName + "」相关的最新方法、论文或开源实现。");
            if (kpNode != null && kpNode.getTeachingTips() != null && !kpNode.getTeachingTips().isBlank()) {
                reading.add("学习提示：" + kpNode.getTeachingTips());
            }
            extendedReading.put("title", kpName + " 拓展阅读建议");
            extendedReading.put("items", reading);
        }
        resources.put("extendedReading", extendedReading);

        // 5. 实操案例：优先用资源Agent 个性化产出（结合专业场景/代码），失败回退模板
        Map<String, Object> agentCase = ResourceAgent.extractResource(resourceAgentData, "practiceCase");
        Map<String, Object> practiceCase = new LinkedHashMap<>();
        if (!agentCase.isEmpty()) {
            practiceCase.put("title", agentCase.getOrDefault("title", kpName + " 实操案例"));
            practiceCase.put("summary", agentCase.getOrDefault("summary", ""));
            practiceCase.put("scenario", agentCase.getOrDefault("scenario", ""));
            practiceCase.put("steps", agentCase.getOrDefault("steps", Collections.emptyList()));
            Object sampleCode = agentCase.get("sampleCode");
            if (sampleCode != null && !String.valueOf(sampleCode).isBlank()) {
                practiceCase.put("sampleCode", sampleCode);
            }
            practiceCase.put("aiGenerated", true);
        } else {
            practiceCase.put("title", kpName + " 实操案例");
            String scene = !pMajor.isEmpty()
                    ? "结合「" + pMajor + "」方向"
                    : (!pCourse.isEmpty() ? "结合《" + pCourse + "》" : "结合真实场景");
            practiceCase.put("scenario", scene + "完成一个应用「" + kpName + "」的小任务。");
            List<String> steps = new ArrayList<>();
            steps.add("第一步：明确任务目标，复述「" + kpName + "」的核心概念与适用条件。");
            steps.add("第二步：" + scene + "，按标准流程动手完成一个最小可运行/可验证的示例。");
            steps.add("第三步：对照结果分析偏差，总结易错点并尝试一个变式。");
            practiceCase.put("steps", steps);
        }
        resources.put("practiceCase", practiceCase);

        // 6. 视频推荐：占位，前端按需异步加载真实视频
        Map<String, Object> video = new LinkedHashMap<>();
        video.put("title", kpName + " 课程视频推荐");
        video.put("content", "系统将从权威课程平台与高校公开课中匹配「" + kpName + "」的讲解视频。");
        resources.put("video", video);

        // 7. 动画讲解脚本：优先用资源Agent 个性化产出，失败回退模板
        Map<String, Object> agentAnim = ResourceAgent.extractResource(resourceAgentData, "animationScript");
        Map<String, Object> animationScript = new LinkedHashMap<>();
        if (!agentAnim.isEmpty()) {
            animationScript.put("title", agentAnim.getOrDefault("title", kpName + " 动画讲解脚本"));
            animationScript.put("summary", agentAnim.getOrDefault("summary", ""));
            animationScript.put("scenes", flattenAnimationScenes(agentAnim, kpName));
            animationScript.put("aiGenerated", true);
        } else {
            List<String> scenes = new ArrayList<>();
            scenes.add("场景一（问题引入）：用一个常见问题引出「" + kpName + "」要解决的核心痛点。");
            scenes.add("场景二（核心机制）：拆解「" + kpName + "」的关键概念与运行原理。");
            scenes.add("场景三（操作步骤）：分步演示如何应用「" + kpName + "」完成任务。");
            scenes.add("场景四（结果反馈）：展示输出结果，强调易错点与检查方法。");
            animationScript.put("title", kpName + " 动画讲解脚本");
            animationScript.put("scenes", scenes);
        }
        resources.put("animationScript", animationScript);

        return resources;
    }

    /** 把资源Agent 的思维导图 branches 结构压平为前端展示用的 nodes 字符串列表。 */
    @SuppressWarnings("unchecked")
    private List<String> flattenMindMapBranches(Map<String, Object> mindMap, String kpName) {
        List<String> nodes = new ArrayList<>();
        Object root = mindMap.get("root");
        nodes.add("核心知识点：" + (root != null && !String.valueOf(root).isBlank() ? root : kpName));
        Object branchesObj = mindMap.get("branches");
        if (branchesObj instanceof List) {
            for (Object b : (List<Object>) branchesObj) {
                if (!(b instanceof Map)) continue;
                Map<String, Object> branch = (Map<String, Object>) b;
                String topic = String.valueOf(branch.getOrDefault("topic", "")).trim();
                Object points = branch.get("points");
                StringBuilder line = new StringBuilder(topic);
                if (points instanceof List && !((List<?>) points).isEmpty()) {
                    line.append("：").append(((List<?>) points).stream()
                            .map(String::valueOf).collect(Collectors.joining("、")));
                }
                if (line.length() > 0) nodes.add(line.toString());
            }
        }
        return nodes.size() > 1 ? nodes : List.of("核心知识点：" + kpName);
    }

    /** 把资源Agent 的拓展阅读 items 结构压平为前端展示用的字符串列表。 */
    @SuppressWarnings("unchecked")
    private List<String> flattenReadingItems(Map<String, Object> reading, String kpName, String moduleName) {
        List<String> items = new ArrayList<>();
        Object itemsObj = reading.get("items");
        if (itemsObj instanceof List) {
            for (Object it : (List<Object>) itemsObj) {
                if (!(it instanceof Map)) continue;
                Map<String, Object> item = (Map<String, Object>) it;
                String topic = String.valueOf(item.getOrDefault("topic", "")).trim();
                String desc = String.valueOf(item.getOrDefault("description", "")).trim();
                if (topic.isEmpty() && desc.isEmpty()) continue;
                items.add(desc.isEmpty() ? topic : topic + "：" + desc);
            }
        }
        return items.isEmpty()
                ? List.of("结合「" + kpName + "」在" + (isBlankText(moduleName) ? "本课程" : moduleName) + "中的应用进行拓展阅读。")
                : items;
    }

    /** 把资源Agent 的动画 scenes 结构压平为前端展示用的字符串列表。 */
    @SuppressWarnings("unchecked")
    private List<String> flattenAnimationScenes(Map<String, Object> anim, String kpName) {
        List<String> scenes = new ArrayList<>();
        Object scenesObj = anim.get("scenes");
        if (scenesObj instanceof List) {
            for (Object s : (List<Object>) scenesObj) {
                if (!(s instanceof Map)) continue;
                Map<String, Object> scene = (Map<String, Object>) s;
                String name = String.valueOf(scene.getOrDefault("scene", "")).trim();
                String narration = String.valueOf(scene.getOrDefault("narration", "")).trim();
                String visual = String.valueOf(scene.getOrDefault("visual", "")).trim();
                StringBuilder line = new StringBuilder();
                if (!name.isEmpty()) line.append(name).append("：");
                if (!narration.isEmpty()) line.append(narration);
                if (!visual.isEmpty()) line.append("（画面：").append(visual).append("）");
                if (line.length() > 0) scenes.add(line.toString());
            }
        }
        return scenes.isEmpty() ? List.of("围绕「" + kpName + "」的动画讲解脚本生成中。") : scenes;
    }

    private String extractConceptContent(Map<String, Object> lesson) {
        Map<String, Object> concept = extractConceptSection(lesson);
        Object content = concept.get("content");
        return content == null ? "" : String.valueOf(content);
    }

    /** 将一段（可能含 Markdown/换行的）长文压缩为纯文本摘要，超过 maxChars 截断并加省略号。 */
    private String summarize(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        String plain = text
                .replaceAll("(?s)```.*?```", " ")           // 去代码块
                .replaceAll("[#>*`_~\\-]", " ")               // 去常见 Markdown 标记
                .replaceAll("\\$\\$?.*?\\$\\$?", " ")        // 去 LaTeX 公式
                .replaceAll("\\s+", " ")                       // 合并空白
                .trim();
        if (plain.length() <= maxChars) {
            return plain;
        }
        return plain.substring(0, maxChars).trim() + "…";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractConceptSection(Map<String, Object> lesson) {
        Object sectionsObj = lesson.get("sections");
        if (sectionsObj instanceof List) {
            for (Object sectionObj : (List<?>) sectionsObj) {
                if (sectionObj instanceof Map
                        && "concept".equals(String.valueOf(((Map<?, ?>) sectionObj).get("type")))) {
                    return (Map<String, Object>) sectionObj;
                }
            }
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractExampleList(Map<String, Object> lesson) {
        Object sectionsObj = lesson.get("sections");
        if (sectionsObj instanceof List) {
            for (Object sectionObj : (List<?>) sectionsObj) {
                if (sectionObj instanceof Map
                        && "example".equals(String.valueOf(((Map<?, ?>) sectionObj).get("type")))) {
                    Object content = ((Map<?, ?>) sectionObj).get("content");
                    if (content instanceof List) {
                        List<Map<String, Object>> out = new ArrayList<>();
                        for (Object e : (List<?>) content) {
                            if (e instanceof Map) {
                                out.add((Map<String, Object>) e);
                            }
                        }
                        return out;
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    private List<String> toStringList(Object obj) {
        List<String> out = new ArrayList<>();
        if (obj instanceof List) {
            for (Object o : (List<?>) obj) {
                String s = o == null ? "" : String.valueOf(o).trim();
                if (!s.isEmpty()) {
                    out.add(s);
                }
            }
        }
        return out;
    }

    private boolean isBlankText(String value) {
        return value == null || value.isBlank();
    }

    private AgentContext buildTeachingContext(Long studentId, String moduleName, String kpId,
                                              String kpName, String kpDesc, double currentMastery,
                                              List<String> errorTypes, String scope) {
        Map<String, Object> profileMap = loadStudentProfileMap(studentId);
        AgentContext context = AgentContext.builder()
                .studentId(studentId)
                .module(moduleName)
                .sessionData(new HashMap<>())
                .knowledgeMastery(new HashMap<>())
                .errorPatterns(loadStudentErrorPatterns(studentId))
                .studentProfile(profileMap)
                .build();
        context.putSessionData("knowledgePointId", kpId);
        context.putSessionData("knowledgePointName", kpName);
        context.putSessionData("knowledgePointDescription", kpDesc);
        context.putSessionData("masteryLevel", currentMastery);
        context.putSessionData("commonErrors", errorTypes);
        context.putSessionData("contentScope", scope);
        context.updateMastery(kpId, currentMastery);
        return context;
    }

    /**
     * 读取学生画像并组装为 prompt 友好的 Map（专业/课程/学历/目标/基础/薄弱模块/兴趣/资源偏好/学习风格）。
     * 用于把赛题要求的「专业、课程、知识短板、学习需求」等个性化信息真正注入大模型上下文。
     * 画像缺失时返回空 Map（各字段判空跳过，退化为仅按掌握度个性化）。
     */
    private Map<String, Object> loadStudentProfileMap(Long studentId) {
        Map<String, Object> map = new HashMap<>();
        try {
            StudentProfile profile = studentProfileRepository.findByStudentId(studentId).orElse(null);
            if (profile == null) {
                return map;
            }
            putIfPresent(map, "majorDirection", profile.getMajorDirection());
            putIfPresent(map, "educationLevel", profile.getEducationLevel());
            putIfPresent(map, "currentCourse", profile.getCurrentCourse());
            putIfPresent(map, "learningGoal", profile.getLearningGoal());
            putIfPresent(map, "foundationLevel", profile.getFoundationLevel());
            putIfPresent(map, "resourcePreference", profile.getResourcePreference());
            putIfPresent(map, "academicInterest", profile.getAcademicInterest());
            putIfPresent(map, "weakModulePriority", profile.getWeakModulePriority());
            putIfPresent(map, "learningStyle", profile.getLearningStyle());
        } catch (Exception e) {
            log.warn("读取学生画像失败, studentId={}: {}", studentId, e.getMessage());
        }
        return map;
    }

    /** 解析学生历史错误模式（StudentProfile.errorPatterns JSON）为 Map&lt;错误类型, 次数&gt;。 */
    private Map<String, Integer> loadStudentErrorPatterns(Long studentId) {
        Map<String, Integer> patterns = new HashMap<>();
        try {
            StudentProfile profile = studentProfileRepository.findByStudentId(studentId).orElse(null);
            if (profile == null || isBlankText(profile.getErrorPatterns())) {
                return patterns;
            }
            Map<String, Object> raw = parseJson(profile.getErrorPatterns(),
                    new TypeReference<Map<String, Object>>() {}, Collections.emptyMap());
            raw.forEach((k, v) -> {
                if (v instanceof Number) {
                    patterns.put(k, ((Number) v).intValue());
                }
            });
        } catch (Exception e) {
            log.warn("解析学生错误模式失败, studentId={}: {}", studentId, e.getMessage());
        }
        return patterns;
    }

    private void putIfPresent(Map<String, Object> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }

    /**
     * 把已执行完的练习 Agent 响应组装为统一的 lessonData（仅含练习块）。
     * 讲解块不在此生成——讲解已改由 {@link #streamLessonExplanation} 流式产出；
     * 多模态资源由 {@link ResourceAgent} 并行产出。
     */
    private AgentResponse buildExerciseResponse(AgentResponse exerciseResp, String kpName) {
        boolean exerciseOk = exerciseResp != null && exerciseResp.isSuccess();

        Map<String, Object> merged = new LinkedHashMap<>();
        merged.put("title", kpName);
        merged.put("strategy", "targeted");
        if (exerciseOk) {
            merged.put("exercises", exerciseResp.getData().getOrDefault("exercises", Collections.emptyList()));
        }
        // 即使练习失败也返回 success：讲解走流式，练习可由 buildQuickExercises 兜底，不应整体降级
        return AgentResponse.success("教学内容生成完成", merged, AgentEvent.LESSON_GENERATED);
    }

    private AgentResponse awaitAgent(CompletableFuture<AgentResponse> future, String kpName, String label) {
        try {
            return future.get(LESSON_AGENT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("TeachingAgent[{}] 生成 {} 超过 {} 秒", label, kpName, LESSON_AGENT_TIMEOUT_SECONDS);
            return AgentResponse.failure(label + "生成超时");
        } catch (Exception e) {
            log.warn("TeachingAgent[{}] 生成 {} 失败: {}", label, kpName, e.getMessage());
            return AgentResponse.failure(label + "生成失败");
        }
    }

    private Map<String, Object> buildQuickLesson(String kpName,
                                                 String kpDesc,
                                                 String strategyLabel,
                                                 List<String> errorTypes,
                                                 String nodeId) {
        Map<String, Object> lesson = new LinkedHashMap<>();
        lesson.put("title", kpName + " — " + strategyLabel);

        List<Map<String, Object>> sections = new ArrayList<>();
        Map<String, Object> conceptSection = new LinkedHashMap<>();
        conceptSection.put("type", "concept");
        conceptSection.put("title", "核心概念");
        conceptSection.put("content", buildQuickConcept(kpName, kpDesc));
        conceptSection.put("summary", kpName + "的基础讲解");
        conceptSection.put("keyFormulas", buildQuickFormulas(kpName));
        sections.add(conceptSection);

        Map<String, Object> exampleSection = new LinkedHashMap<>();
        exampleSection.put("type", "example");
        exampleSection.put("title", "典型例题");
        exampleSection.put("content", buildQuickExamples(kpName));
        sections.add(exampleSection);

        if (errorTypes != null && !errorTypes.isEmpty()) {
            Map<String, Object> mistakeSection = new LinkedHashMap<>();
            mistakeSection.put("type", "key_points");
            mistakeSection.put("title", "常见错误提醒");
            mistakeSection.put("content", errorTypes.stream()
                    .limit(3)
                    .map(error -> "容易出错点：" + error + "\n纠正建议：先回到定义和适用条件，再代入题目检查。")
                    .collect(Collectors.joining("\n\n")));
            sections.add(mistakeSection);
        }

        List<Map<String, Object>> exercises = buildQuickExercises(kpName, nodeId);
        lesson.put("sections", sections);
        lesson.put("exercises", exercises);
        lesson.put("totalExercises", exercises.size());
        lesson.put("summary", "已先给出快速学习内容，避免等待过久；需要更细讲解可继续询问 AI 路径伴学。");
        return lesson;
    }

    private String buildQuickConcept(String kpName, String kpDesc) {
        String description = kpDesc == null || kpDesc.isBlank()
                ? "先明确概念定义、适用条件和常见题型，再通过例题检查是否能独立使用。"
                : kpDesc;
        return "本节点先用快速讲解帮你进入学习状态。\n\n"
                + "- 知识点：" + kpName + "\n"
                + "- 核心理解：" + description + "\n"
                + "- 学习顺序：先看定义和条件，再看例题步骤，最后做练习确认是否真正掌握。";
    }

    private List<String> buildQuickFormulas(String kpName) {
        if (kpName != null && kpName.contains("基本不等式")) {
            return List.of("$$\\frac{a+b}{2} \\ge \\sqrt{ab}$$", "等号成立条件：$$a=b$$");
        }
        return Collections.emptyList();
    }

    private List<Map<String, Object>> buildQuickExamples(String kpName) {
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("title", kpName + "入门例题");
        example.put("problem", "请说明学习「" + kpName + "」时，为什么不能只记结论，还要检查适用条件？");
        example.put("solution", "步骤1：先写出该知识点的定义或公式。\n步骤2：标出公式成立需要满足的条件。\n步骤3：代入题目数据前，先确认条件是否满足。\n步骤4：再进行计算或推理。");
        example.put("keyPoint", "先验条件比直接套公式更重要。");
        return List.of(example);
    }

    private List<Map<String, Object>> buildQuickExercises(String kpName, String nodeId) {
        List<Map<String, Object>> exercises = new ArrayList<>();
        exercises.add(buildQuickExercise(nodeId + "_quick_1",
                "学习「" + kpName + "」时，第一步最应该做什么？",
                "A", "明确定义和适用条件", "直接背答案", "跳过例题", "只看最终结果"));
        exercises.add(buildQuickExercise(nodeId + "_quick_2",
                "如果一道题做错了，最有效的订正方式是？",
                "B", "只改最终答案", "回到条件、公式和步骤逐项检查", "马上做更难的题", "忽略错误原因"));
        return exercises;
    }

    private Map<String, Object> buildQuickExercise(String id,
                                                   String problem,
                                                   String correctAnswer,
                                                   String optionA,
                                                   String optionB,
                                                   String optionC,
                                                   String optionD) {
        Map<String, Object> exercise = new LinkedHashMap<>();
        exercise.put("id", id);
        exercise.put("problem", problem);
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("A", optionA);
        options.put("B", optionB);
        options.put("C", optionC);
        options.put("D", optionD);
        exercise.put("options", options);
        exercise.put("correctAnswer", correctAnswer);
        exercise.put("explanation", "这题用于快速检查你是否理解学习步骤，后续可让 AI 继续生成更贴合的变式题。");
        exercise.put("difficulty", 1);
        return exercise;
    }

    /**
     * 单独匹配视频资源，避免拖慢课程正文加载。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> findLessonVideo(Long studentId, Long pathId, String nodeId) {
        LearningPath path = learningPathRepository.findById(pathId)
                .orElseThrow(() -> new BusinessException(404, "学习路径不存在"));

        if (!path.getStudentId().equals(studentId)) {
            throw new BusinessException(403, "无权访问该学习路径");
        }

        List<Map<String, Object>> nodes = parseJson(path.getNodes(),
                new TypeReference<List<Map<String, Object>>>() {}, Collections.emptyList());

        Map<String, Object> targetNode = nodes.stream()
                .filter(n -> nodeId.equals(n.get("nodeId")))
                .findFirst()
                .orElseThrow(() -> new BusinessException(404, "节点不存在"));

        Map<String, Object> existingVideo = findVideoSnapshot(path, nodeId);
        if (!existingVideo.isEmpty()) {
            return existingVideo;
        }

        String kpId = (String) targetNode.getOrDefault("knowledgePointId", nodeId);
        String kpName = (String) targetNode.getOrDefault("knowledgePointName", resolveKpName(kpId));
        KnowledgeNode kpNode = knowledgeGraphService.getNode(kpId);
        String moduleName = kpNode != null ? kpNode.getModule() : "高校课程";

        Map<String, Object> videoResource = bilibiliVideoService.findBestVideo(
                kpId, kpName, moduleName, "视频讲解");
        if (videoResource == null || videoResource.isEmpty()) {
            return Collections.emptyMap();
        }

        saveVideoSnapshot(path, nodeId, videoResource);
        return videoResource;
    }

    /** 资源详情卡片类型（与前端卡片一一对应，不含已单独懒加载的 video）。 */
    private static final List<String> RESOURCE_DETAIL_TYPES = Arrays.asList(
            "document", "mindMap", "extendedReading", "practiceCase", "animationScript");

    /**
     * 懒加载某节点各资源卡片的「详细内容」。进入节点后由前端异步请求一次，
     * AI 基于已生成的课程内容为每张卡片产出可深入学习的 Markdown 详情，并写入快照缓存。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> findLessonResourceDetails(Long studentId, Long pathId, String nodeId) {
        LearningPath path = learningPathRepository.findById(pathId)
                .orElseThrow(() -> new BusinessException(404, "学习路径不存在"));
        if (!path.getStudentId().equals(studentId)) {
            throw new BusinessException(403, "无权访问该学习路径");
        }

        // 1. 命中快照直接返回
        Map<String, Object> cached = findResourceDetailsSnapshot(path, nodeId);
        if (!cached.isEmpty()) {
            return cached;
        }

        // 2. 取课程快照作为详情生成的素材
        Map<String, Object> lessonResponse = findLessonSnapshotResponse(path, nodeId);
        if (lessonResponse.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> lesson = lessonResponse.get("lesson") instanceof Map
                ? (Map<String, Object>) lessonResponse.get("lesson")
                : Collections.emptyMap();
        String kpName = firstNonBlank(
                String.valueOf(lessonResponse.getOrDefault("knowledgePointName", "")),
                String.valueOf(lessonResponse.getOrDefault("title", "")),
                "当前知识点");

        // 3. 优先复用资源Agent 在课程生成阶段产出的结构化资源，组装为详情，避免重复调用大模型
        Map<String, Object> resourceAgentData = lessonResponse.get("resourceAgentData") instanceof Map
                ? (Map<String, Object>) lessonResponse.get("resourceAgentData")
                : Collections.emptyMap();
        Map<String, Object> details = assembleResourceDetailsFromAgent(kpName, lesson, resourceAgentData);

        // 4. 资源Agent 数据不足时，退化为按课程素材整体生成；再不行用课程数据兜底
        if (details.size() < RESOURCE_DETAIL_TYPES.size()) {
            Map<String, Object> aiDetails = generateResourceDetailsWithAi(kpName, lesson);
            for (Map.Entry<String, Object> e : aiDetails.entrySet()) {
                details.putIfAbsent(e.getKey(), e.getValue());
            }
        }
        if (details.isEmpty()) {
            details = assembleResourceDetailsFallback(kpName, lesson);
        }
        if (details.isEmpty()) {
            return Collections.emptyMap();
        }

        saveResourceDetailsSnapshot(path, nodeId, details);
        return details;
    }

    /**
     * 生成（或读取缓存）某节点的动画资产。
     * <p>
     * 资源 Agent 已产出结构化 animationScript，本方法将其接到动画生成 AI。
     * 未配置外部文生视频服务时返回 runtime-svg 资产，前端用同一份分镜实时渲染可播放动画。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> findLessonAnimation(Long studentId, Long pathId, String nodeId) {
        LearningPath path = learningPathRepository.findById(pathId)
                .orElseThrow(() -> new BusinessException(404, "学习路径不存在"));
        if (!path.getStudentId().equals(studentId)) {
            throw new BusinessException(403, "无权访问该学习路径");
        }

        Map<String, Object> cached = findAnimationSnapshot(path, nodeId);
        if (!cached.isEmpty()) {
            return cached;
        }

        Map<String, Object> lessonResponse = findLessonSnapshotResponse(path, nodeId);
        if (lessonResponse.isEmpty()) {
            throw new BusinessException(400, "请先打开课程节点，生成学习资源后再生成动画");
        }

        String kpName = firstNonBlank(
                String.valueOf(lessonResponse.getOrDefault("knowledgePointName", "")),
                String.valueOf(lessonResponse.getOrDefault("title", "")),
                "当前知识点");
        String moduleName = "";
        Object kpObj = lessonResponse.get("knowledgePoint");
        if (kpObj instanceof Map) {
            moduleName = String.valueOf(((Map<String, Object>) kpObj).getOrDefault("module", ""));
        }

        Map<String, Object> resourceAgentData = lessonResponse.get("resourceAgentData") instanceof Map
                ? (Map<String, Object>) lessonResponse.get("resourceAgentData")
                : Collections.emptyMap();
        Map<String, Object> anim = ResourceAgent.extractResource(resourceAgentData, "animationScript");
        List<Map<String, Object>> scenes = normalizeAnimationScenes(anim);
        if (scenes.isEmpty()) {
            Object resourcesObj = lessonResponse.get("resources");
            if (resourcesObj instanceof Map) {
                Map<String, Object> resources = (Map<String, Object>) resourcesObj;
                Object animationObj = resources.get("animationScript");
                if (animationObj instanceof Map) {
                    scenes = normalizeAnimationScenes((Map<String, Object>) animationObj);
                }
            }
        }
        if (scenes.isEmpty()) {
            scenes = buildFallbackAnimationScenes(kpName);
        }

        Map<String, Object> asset = animationAiService.generateAnimation(kpName, moduleName, scenes);
        saveAnimationSnapshot(path, nodeId, asset);
        return asset;
    }

    /**
     * 用资源Agent 的结构化产出 + 讲解全文，组装资源卡片详情（Markdown）。
     * document 用流式讲解回填的概念全文；mindMap/extendedReading/practiceCase/animationScript
     * 由资源Agent 的结构化数据渲染为 Markdown。缺失的类型留空，交由调用方补齐。
     */
    private Map<String, Object> assembleResourceDetailsFromAgent(String kpName,
                                                                 Map<String, Object> lesson,
                                                                 Map<String, Object> resourceAgentData) {
        Map<String, Object> details = new LinkedHashMap<>();

        // document：用概念讲解全文（流式回填的 concept.content）
        String conceptContent = extractConceptContent(lesson);
        if (conceptContent != null && !conceptContent.isBlank()) {
            details.put("document", detailEntry(kpName + " 讲解文档", conceptContent));
        }

        if (resourceAgentData == null || resourceAgentData.isEmpty()) {
            return details;
        }

        Map<String, Object> mindMap = ResourceAgent.extractResource(resourceAgentData, "mindMap");
        if (!mindMap.isEmpty()) {
            details.put("mindMap", detailEntry(
                    String.valueOf(mindMap.getOrDefault("title", kpName + " 思维导图")),
                    renderMindMapMarkdown(mindMap)));
        }
        Map<String, Object> reading = ResourceAgent.extractResource(resourceAgentData, "extendedReading");
        if (!reading.isEmpty()) {
            details.put("extendedReading", detailEntry(
                    String.valueOf(reading.getOrDefault("title", kpName + " 拓展阅读")),
                    renderReadingMarkdown(reading)));
        }
        Map<String, Object> pc = ResourceAgent.extractResource(resourceAgentData, "practiceCase");
        if (!pc.isEmpty()) {
            details.put("practiceCase", detailEntry(
                    String.valueOf(pc.getOrDefault("title", kpName + " 实操案例")),
                    renderPracticeCaseMarkdown(pc)));
        }
        Map<String, Object> anim = ResourceAgent.extractResource(resourceAgentData, "animationScript");
        if (!anim.isEmpty()) {
            Map<String, Object> animEntry = detailEntry(
                    String.valueOf(anim.getOrDefault("title", kpName + " 动画脚本")),
                    renderAnimationMarkdown(anim));
            // 额外保留结构化分镜，供前端动画播放器逐场景演示（content 仍是 Markdown 兜底）
            animEntry.put("scenes", normalizeAnimationScenes(anim));
            details.put("animationScript", animEntry);
        }
        return details;
    }

    @SuppressWarnings("unchecked")
    private String renderMindMapMarkdown(Map<String, Object> mindMap) {
        StringBuilder sb = new StringBuilder();
        Object root = mindMap.get("root");
        sb.append("# ").append(root != null ? root : mindMap.getOrDefault("title", "知识结构")).append("\n\n");
        Object branches = mindMap.get("branches");
        if (branches instanceof List) {
            for (Object b : (List<Object>) branches) {
                if (!(b instanceof Map)) continue;
                Map<String, Object> branch = (Map<String, Object>) b;
                sb.append("## ").append(branch.getOrDefault("topic", "")).append("\n");
                Object points = branch.get("points");
                if (points instanceof List) {
                    for (Object p : (List<Object>) points) {
                        sb.append("- ").append(p).append("\n");
                    }
                }
                sb.append("\n");
            }
        }
        return sb.toString().trim();
    }

    @SuppressWarnings("unchecked")
    private String renderReadingMarkdown(Map<String, Object> reading) {
        StringBuilder sb = new StringBuilder();
        Object items = reading.get("items");
        if (items instanceof List) {
            int i = 1;
            for (Object it : (List<Object>) items) {
                if (!(it instanceof Map)) continue;
                Map<String, Object> item = (Map<String, Object>) it;
                sb.append("### ").append(i++).append(". ").append(item.getOrDefault("topic", "")).append("\n");
                Object desc = item.get("description");
                if (desc != null && !String.valueOf(desc).isBlank()) {
                    sb.append(desc).append("\n\n");
                }
            }
        }
        return sb.toString().trim();
    }

    @SuppressWarnings("unchecked")
    private String renderPracticeCaseMarkdown(Map<String, Object> pc) {
        StringBuilder sb = new StringBuilder();
        Object scenario = pc.get("scenario");
        if (scenario != null && !String.valueOf(scenario).isBlank()) {
            sb.append("## 应用场景\n").append(scenario).append("\n\n");
        }
        Object steps = pc.get("steps");
        if (steps instanceof List && !((List<?>) steps).isEmpty()) {
            sb.append("## 操作步骤\n");
            int i = 1;
            for (Object s : (List<Object>) steps) {
                sb.append(i++).append(". ").append(s).append("\n");
            }
            sb.append("\n");
        }
        Object code = pc.get("sampleCode");
        if (code != null && !String.valueOf(code).isBlank()) {
            sb.append("## 示例代码\n```\n").append(code).append("\n```\n");
        }
        Object expected = pc.get("expectedResult");
        if (expected != null && !String.valueOf(expected).isBlank()) {
            sb.append("## 预期结果与自检\n").append(expected).append("\n");
        }
        return sb.toString().trim();
    }

    @SuppressWarnings("unchecked")
    private String renderAnimationMarkdown(Map<String, Object> anim) {
        StringBuilder sb = new StringBuilder();
        Object scenes = anim.get("scenes");
        if (scenes instanceof List) {
            int i = 1;
            for (Object s : (List<Object>) scenes) {
                if (!(s instanceof Map)) continue;
                Map<String, Object> scene = (Map<String, Object>) s;
                sb.append("## 场景").append(i++).append("：").append(scene.getOrDefault("scene", "")).append("\n");
                Object narration = scene.get("narration");
                if (narration != null && !String.valueOf(narration).isBlank()) {
                    sb.append("**旁白**：").append(narration).append("\n\n");
                }
                Object visual = scene.get("visual");
                if (visual != null && !String.valueOf(visual).isBlank()) {
                    sb.append("**画面**：").append(visual).append("\n\n");
                }
            }
        }
        return sb.toString().trim();
    }

    /** 规整动画分镜为结构化列表 [{scene,narration,visual,diagram}]，供前端播放器逐帧演示。 */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeAnimationScenes(Map<String, Object> anim) {
        List<Map<String, Object>> result = new ArrayList<>();
        Object scenes = anim.get("scenes");
        if (scenes instanceof List) {
            for (Object s : (List<Object>) scenes) {
                if (!(s instanceof Map)) continue;
                Map<String, Object> scene = (Map<String, Object>) s;
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("scene", String.valueOf(scene.getOrDefault("scene", "")).trim());
                entry.put("narration", String.valueOf(scene.getOrDefault("narration", "")).trim());
                entry.put("visual", String.valueOf(scene.getOrDefault("visual", "")).trim());
                Map<String, Object> diagram = normalizeDiagram(scene.get("diagram"));
                if (diagram != null) {
                    entry.put("diagram", diagram);
                }
                if (!entry.get("scene").toString().isEmpty()
                        || !entry.get("narration").toString().isEmpty()
                        || !entry.get("visual").toString().isEmpty()) {
                    result.add(entry);
                }
            }
        }
        return result;
    }

    /** 校验并规整单个场景的流程图图元：{type, nodes:[{id,label}], edges:[{from,to,label}]}。无效返回 null。 */
    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeDiagram(Object diagramObj) {
        if (!(diagramObj instanceof Map)) return null;
        Map<String, Object> diagram = (Map<String, Object>) diagramObj;
        Object nodesObj = diagram.get("nodes");
        Object edgesObj = diagram.get("edges");
        if (!(nodesObj instanceof List) || ((List<?>) nodesObj).isEmpty()) return null;

        List<Map<String, Object>> nodes = new ArrayList<>();
        for (Object n : (List<Object>) nodesObj) {
            if (!(n instanceof Map)) continue;
            Map<String, Object> node = (Map<String, Object>) n;
            String id = String.valueOf(node.getOrDefault("id", "")).trim();
            String label = String.valueOf(node.getOrDefault("label", "")).trim();
            if (id.isEmpty()) continue;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", id);
            entry.put("label", label.isEmpty() ? id : label);
            nodes.add(entry);
        }
        if (nodes.isEmpty()) return null;

        List<Map<String, Object>> edges = new ArrayList<>();
        if (edgesObj instanceof List) {
            for (Object e : (List<Object>) edgesObj) {
                if (!(e instanceof Map)) continue;
                Map<String, Object> edge = (Map<String, Object>) e;
                String from = String.valueOf(edge.getOrDefault("from", "")).trim();
                String to = String.valueOf(edge.getOrDefault("to", "")).trim();
                if (from.isEmpty() || to.isEmpty()) continue;
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("from", from);
                entry.put("to", to);
                entry.put("label", String.valueOf(edge.getOrDefault("label", "")).trim());
                edges.add(entry);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", String.valueOf(diagram.getOrDefault("type", "flow")).trim());
        result.put("nodes", nodes);
        result.put("edges", edges);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> generateResourceDetailsWithAi(String kpName, Map<String, Object> lesson) {
        try {
            Map<String, Object> concept = extractConceptSection(lesson);
            List<Map<String, Object>> examples = extractExampleList(lesson);
            String summary = String.valueOf(lesson.getOrDefault("summary", ""));

            StringBuilder material = new StringBuilder();
            material.append("# 知识点：").append(kpName).append("\n");
            if (!String.valueOf(concept.getOrDefault("summary", "")).isBlank()) {
                material.append("## 概念概要\n").append(concept.get("summary")).append("\n");
            }
            if (!String.valueOf(concept.getOrDefault("content", "")).isBlank()) {
                material.append("## 概念讲解\n").append(truncate(String.valueOf(concept.get("content")), 1200)).append("\n");
            }
            List<String> keyFormulas = toStringList(concept.get("keyFormulas"));
            if (!keyFormulas.isEmpty()) {
                material.append("## 关键公式/要点\n- ").append(String.join("\n- ", keyFormulas)).append("\n");
            }
            if (!String.valueOf(concept.getOrDefault("visualDescription", "")).isBlank()) {
                material.append("## 图示描述\n").append(concept.get("visualDescription")).append("\n");
            }
            for (int i = 0; i < examples.size() && i < 3; i++) {
                Map<String, Object> ex = examples.get(i);
                material.append("## 例题").append(i + 1).append("：").append(ex.getOrDefault("title", "")).append("\n");
                material.append("题目：").append(ex.getOrDefault("problem", ex.getOrDefault("question", ""))).append("\n");
                material.append("考点：").append(ex.getOrDefault("keyPoint", "")).append("\n");
            }
            if (!summary.isBlank()) {
                material.append("## 本节要点\n").append(summary).append("\n");
            }

            String sys = "你是高校课程的资源详情生成器。基于给定的课程素材，为 5 张学习资源卡片分别生成"
                    + "“可深入学习的详细内容”，要求具体、有实操价值、贴合该知识点，避免空话套话。"
                    + "这 5 张卡片会同时展示给同一学生，必须各司其职、内容互不重叠。"
                    + "只输出严格 JSON，键为：document、mindMap、extendedReading、practiceCase、animationScript；"
                    + "每个值为对象 {\"title\":\"卡片标题\",\"content\":\"Markdown 正文\"}。"
                    + "各卡片职责边界（严禁重叠）："
                    + "document=系统精讲(定义/原理/分点展开/小结)，是唯一负责“讲清概念”的卡；"
                    + "mindMap=只用 Markdown 多级列表呈现知识结构骨架(核心→分支→要点)，不展开讲解、不举例；"
                    + "extendedReading=只给指向外部的资料清单(3-5 条具体的书籍章节/论文/官方文档/开源项目/公开课名称与检索关键词)，是“去哪继续学”的索引，不自己讲内容；"
                    + "practiceCase=只给一个可动手完成的具体任务(明确输入→步骤→示例代码或演算→预期结果与自检)，不泛泛说“应用到某场景”；"
                    + "animationScript=只给分镜化的过程演示(场景/画面/旁白)，重点是动态展示过程怎么走，不罗列知识要点。"
                    + "公式用 $...$ 包裹，不要用 ASCII 字符画。";
            String user = "## 课程素材\n" + material + "\n请据此生成 5 张卡片的详细内容，确保 5 张内容互不重复。";

            String resp = llmService.chatJsonSync(user, sys, 0.6);
            Map<String, Object> data = objectMapper.readValue(resp,
                    new TypeReference<Map<String, Object>>() {});

            Map<String, Object> details = new LinkedHashMap<>();
            for (String type : RESOURCE_DETAIL_TYPES) {
                Object obj = data.get(type);
                if (obj instanceof Map) {
                    Map<String, Object> card = (Map<String, Object>) obj;
                    String content = String.valueOf(card.getOrDefault("content", "")).trim();
                    if (!content.isBlank()) {
                        Map<String, Object> entry = new LinkedHashMap<>();
                        entry.put("title", firstNonBlank(String.valueOf(card.getOrDefault("title", "")), kpName));
                        entry.put("content", content);
                        details.put(type, entry);
                    }
                }
            }
            return details;
        } catch (Exception e) {
            log.warn("AI 生成资源详情失败，使用兜底组装: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /** AI 不可用时，用已生成的课程数据组装基础详情，保证详情页非空。 */
    private Map<String, Object> assembleResourceDetailsFallback(String kpName, Map<String, Object> lesson) {
        Map<String, Object> details = new LinkedHashMap<>();
        Map<String, Object> concept = extractConceptSection(lesson);
        String conceptContent = String.valueOf(concept.getOrDefault("content", ""));
        if (!conceptContent.isBlank()) {
            details.put("document", detailEntry(kpName + " 精讲", conceptContent));
        }
        List<Map<String, Object>> examples = extractExampleList(lesson);
        if (!examples.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> ex : examples) {
                sb.append("### ").append(ex.getOrDefault("title", "实操案例")).append("\n\n");
                sb.append("**任务**：").append(ex.getOrDefault("problem", ex.getOrDefault("question", ""))).append("\n\n");
                Object solution = ex.get("solution");
                if (solution != null && !String.valueOf(solution).isBlank()) {
                    sb.append("**参考思路**：\n").append(solution).append("\n\n");
                }
                if (!String.valueOf(ex.getOrDefault("keyPoint", "")).isBlank()) {
                    sb.append("**关键点**：").append(ex.get("keyPoint")).append("\n\n");
                }
            }
            details.put("practiceCase", detailEntry(kpName + " 实操案例", sb.toString().trim()));
        }
        return details;
    }

    private Map<String, Object> detailEntry(String title, String content) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("title", title);
        entry.put("content", content);
        return entry;
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findResourceDetailsSnapshot(LearningPath path, String nodeId) {
        Map<String, Object> snapshots = parseJson(path.getLessonSnapshots(),
                new TypeReference<Map<String, Object>>() {}, Collections.emptyMap());
        Object lessonObj = snapshots.get(nodeId);
        if (!(lessonObj instanceof Map)) {
            return Collections.emptyMap();
        }
        Object detailsObj = ((Map<String, Object>) lessonObj).get("resourceDetails");
        if (detailsObj instanceof Map && !((Map<?, ?>) detailsObj).isEmpty()) {
            return new LinkedHashMap<>((Map<String, Object>) detailsObj);
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private void saveResourceDetailsSnapshot(LearningPath path, String nodeId, Map<String, Object> details) {
        Map<String, Object> snapshots = new LinkedHashMap<>(parseJson(path.getLessonSnapshots(),
                new TypeReference<Map<String, Object>>() {}, Collections.emptyMap()));
        Map<String, Object> lessonSnapshot = new LinkedHashMap<>();
        Object existing = snapshots.get(nodeId);
        if (existing instanceof Map) {
            lessonSnapshot.putAll((Map<String, Object>) existing);
        }
        lessonSnapshot.put("resourceDetails", details);
        snapshots.put(nodeId, lessonSnapshot);
        try {
            path.setLessonSnapshots(objectMapper.writeValueAsString(snapshots));
            learningPathRepository.save(path);
        } catch (Exception e) {
            throw new BusinessException(500, "保存资源详情快照失败");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findAnimationSnapshot(LearningPath path, String nodeId) {
        Map<String, Object> snapshots = parseJson(path.getLessonSnapshots(),
                new TypeReference<Map<String, Object>>() {}, Collections.emptyMap());
        Object lessonObj = snapshots.get(nodeId);
        if (!(lessonObj instanceof Map)) {
            return Collections.emptyMap();
        }
        Object animationObj = ((Map<String, Object>) lessonObj).get("animationAsset");
        if (animationObj instanceof Map && !((Map<?, ?>) animationObj).isEmpty()) {
            return new LinkedHashMap<>((Map<String, Object>) animationObj);
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private void saveAnimationSnapshot(LearningPath path, String nodeId, Map<String, Object> animationAsset) {
        Map<String, Object> snapshots = new LinkedHashMap<>(parseJson(path.getLessonSnapshots(),
                new TypeReference<Map<String, Object>>() {}, Collections.emptyMap()));
        Map<String, Object> lessonSnapshot = new LinkedHashMap<>();
        Object existing = snapshots.get(nodeId);
        if (existing instanceof Map) {
            lessonSnapshot.putAll((Map<String, Object>) existing);
        }
        lessonSnapshot.put("animationAsset", animationAsset);
        snapshots.put(nodeId, lessonSnapshot);
        try {
            path.setLessonSnapshots(objectMapper.writeValueAsString(snapshots));
            path.setLastStudyAt(LocalDateTime.now());
            learningPathRepository.save(path);
        } catch (Exception e) {
            throw new BusinessException(500, "保存动画快照失败");
        }
    }

    private List<Map<String, Object>> buildFallbackAnimationScenes(String kpName) {
        List<Map<String, Object>> scenes = new ArrayList<>();
        scenes.add(animationScene("问题引入",
                "先观察一个具体问题，明确为什么需要学习「" + kpName + "」。",
                "从问题输入进入分析流程，标出待解决的关键矛盾。",
                diagram(List.of("问题", "条件", "目标"), List.of(new String[]{"a", "b", "分析"}, new String[]{"b", "c", "锁定"}))));
        scenes.add(animationScene("核心机制",
                "把「" + kpName + "」拆成核心概念、判断条件和处理规则。",
                "节点依次点亮，展示从概念到规则的推理链。",
                diagram(List.of("概念", "条件", "规则", "结论"), List.of(new String[]{"a", "b", ""}, new String[]{"b", "c", ""}, new String[]{"c", "d", "推出"}))));
        scenes.add(animationScene("操作演示",
                "按步骤应用规则，先检查条件，再执行计算或推理。",
                "流程箭头按顺序流动，强调每一步的输入与输出。",
                diagram(List.of("输入", "检查", "处理", "输出"), List.of(new String[]{"a", "b", ""}, new String[]{"b", "c", "通过"}, new String[]{"c", "d", ""}))));
        scenes.add(animationScene("结果反馈",
                "最后对结果做自检，回看容易出错的位置。",
                "正确路径高亮，错误分支淡出，形成学习闭环。",
                diagram(List.of("结果", "自检", "纠错", "掌握"), List.of(new String[]{"a", "b", ""}, new String[]{"b", "c", "偏差"}, new String[]{"c", "d", "修正"}))));
        return scenes;
    }

    private Map<String, Object> animationScene(String scene,
                                               String narration,
                                               String visual,
                                               Map<String, Object> diagram) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("scene", scene);
        item.put("narration", narration);
        item.put("visual", visual);
        item.put("diagram", diagram);
        return item;
    }

    private Map<String, Object> diagram(List<String> labels, List<String[]> edgeSpecs) {
        Map<String, Object> diagram = new LinkedHashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", String.valueOf((char) ('a' + i)));
            node.put("label", labels.get(i));
            nodes.add(node);
        }
        List<Map<String, Object>> edges = new ArrayList<>();
        for (String[] spec : edgeSpecs) {
            if (spec.length < 2) continue;
            Map<String, Object> edge = new LinkedHashMap<>();
            edge.put("from", spec[0]);
            edge.put("to", spec[1]);
            edge.put("label", spec.length > 2 ? spec[2] : "");
            edges.add(edge);
        }
        diagram.put("type", "flow");
        diagram.put("nodes", nodes);
        diagram.put("edges", edges);
        return diagram;
    }

    // ====================================================================== 个性化演示文稿（PPT）

    /**
     * 生成（或命中快照返回）某节点的个性化演示文稿大纲（slides JSON）。
     * <p>
     * 复用已生成的课程快照素材（讲解全文 + 例题 + 小结 + 思维导图）作为 PresentationAgent 的输入，
     * 据学生画像与掌握度重组为结构化幻灯片，供前端 reveal.js 在线演示与 .pptx 导出共用。
     * 命中快照秒回；生成失败或质量不足时回退到基于课程素材的模板大纲兜底。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> findLessonSlides(Long studentId, Long pathId, String nodeId) {
        LearningPath path = learningPathRepository.findById(pathId)
                .orElseThrow(() -> new BusinessException(404, "学习路径不存在"));
        if (!path.getStudentId().equals(studentId)) {
            throw new BusinessException(403, "无权访问该学习路径");
        }

        // 1. 命中快照直接返回
        Map<String, Object> cached = findSlidesSnapshot(path, nodeId);
        if (!cached.isEmpty()) {
            return cached;
        }

        // 2. 取课程快照作为演示文稿生成的素材
        Map<String, Object> lessonResponse = findLessonSnapshotResponse(path, nodeId);
        if (lessonResponse.isEmpty()) {
            throw new BusinessException(409, "请先打开该节点完成课程内容生成，再生成演示文稿");
        }
        Map<String, Object> lesson = lessonResponse.get("lesson") instanceof Map
                ? (Map<String, Object>) lessonResponse.get("lesson")
                : Collections.emptyMap();
        String kpName = firstNonBlank(
                String.valueOf(lessonResponse.getOrDefault("knowledgePointName", "")),
                String.valueOf(lessonResponse.getOrDefault("title", "")),
                "当前知识点");
        Map<String, Object> kpMap = lessonResponse.get("knowledgePoint") instanceof Map
                ? (Map<String, Object>) lessonResponse.get("knowledgePoint")
                : Collections.emptyMap();
        String kpId = String.valueOf(kpMap.getOrDefault("id", nodeId));
        Map<String, Object> pathNode = findPathNode(path, nodeId);
        String moduleName = firstNonBlank(pathNode.get("module"), kpMap.get("module"), "高校课程");
        String kpDesc = "";
        KnowledgeNode kpNode = knowledgeGraphService.getNode(kpId);
        if (kpNode != null && kpNode.getDescription() != null) {
            kpDesc = kpNode.getDescription();
        }
        double mastery = 0.5;
        Object masteryObj = lessonResponse.get("studentMastery");
        if (masteryObj instanceof Number) {
            mastery = ((Number) masteryObj).doubleValue();
        }
        Map<String, Object> resourceAgentData = lessonResponse.get("resourceAgentData") instanceof Map
                ? (Map<String, Object>) lessonResponse.get("resourceAgentData")
                : Collections.emptyMap();

        // 3. 汇总课程素材喂给 PresentationAgent
        String material = buildSlideMaterial(kpName, lesson, resourceAgentData);
        AgentContext context = buildTeachingContext(studentId, moduleName, kpId, kpName,
                kpDesc, mastery, Collections.emptyList(), "presentation");
        context.putSessionData("lessonMaterial", material);

        Map<String, Object> slidesDoc;
        AgentResponse response = presentationAgent.execute(context);
        if (response != null && response.isSuccess() && response.getData() != null
                && response.getData().get("slides") instanceof List) {
            slidesDoc = new LinkedHashMap<>(response.getData());
            slidesDoc.put("degraded", false);
        } else {
            log.warn("PresentationAgent 生成失败，回退模板大纲, kpName={}, msg={}",
                    kpName, response != null ? response.getMessage() : "null");
            slidesDoc = buildFallbackSlides(kpName, lesson, resourceAgentData);
            slidesDoc.put("degraded", true);
        }
        slidesDoc = normalizeSlidesDoc(slidesDoc, kpName, moduleName);
        slidesDoc.put("pathId", pathId);
        slidesDoc.put("nodeId", nodeId);
        slidesDoc.put("generatedAt", LocalDateTime.now().toString());

        // 4. 标准化后的兜底文稿也可缓存，保证预览和 .pptx 下载使用同一份内容。
        saveSlidesSnapshot(path, nodeId, slidesDoc);
        return slidesDoc;
    }

    /** 汇总讲解全文 + 例题 + 小结 + 思维导图，作为演示文稿生成的素材。 */
    @SuppressWarnings("unchecked")
    private String buildSlideMaterial(String kpName, Map<String, Object> lesson,
                                      Map<String, Object> resourceAgentData) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 知识点：").append(kpName).append("\n");

        Map<String, Object> concept = extractConceptSection(lesson);
        if (!String.valueOf(concept.getOrDefault("summary", "")).isBlank()) {
            sb.append("## 概要\n").append(concept.get("summary")).append("\n");
        }
        String conceptContent = String.valueOf(concept.getOrDefault("content", ""));
        if (!conceptContent.isBlank() && !"null".equals(conceptContent)) {
            sb.append("## 概念讲解\n").append(truncate(conceptContent, 2000)).append("\n");
        }
        List<String> keyFormulas = toStringList(concept.get("keyFormulas"));
        if (!keyFormulas.isEmpty()) {
            sb.append("## 关键公式/要点\n- ").append(String.join("\n- ", keyFormulas)).append("\n");
        }

        List<Map<String, Object>> examples = extractExampleList(lesson);
        if (!examples.isEmpty()) {
            sb.append("## 典型例题\n");
            int i = 1;
            for (Map<String, Object> ex : examples) {
                if (i > 2) break;
                sb.append(i++).append(". ").append(ex.getOrDefault("title", ""))
                        .append("：").append(ex.getOrDefault("problem", "")).append("\n");
            }
        }

        Object summary = lesson.get("summary");
        if (summary != null && !String.valueOf(summary).isBlank()) {
            sb.append("## 本节小结\n").append(summary).append("\n");
        }

        Map<String, Object> mindMap = ResourceAgent.extractResource(resourceAgentData, "mindMap");
        if (!mindMap.isEmpty()) {
            sb.append("## 知识结构（思维导图）\n").append(renderMindMapMarkdown(mindMap)).append("\n");
        }
        Map<String, Object> pc = ResourceAgent.extractResource(resourceAgentData, "practiceCase");
        if (!pc.isEmpty()) {
            sb.append("## 实操案例\n").append(renderPracticeCaseMarkdown(pc)).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * 模板兜底：PresentationAgent 失败时，用已生成的课程素材拼出一份可用的演示文稿大纲。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> buildFallbackSlides(String kpName, Map<String, Object> lesson,
                                                    Map<String, Object> resourceAgentData) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("title", kpName);
        meta.put("subtitle", "个性化学习演示");
        meta.put("theme", "tech-blue");
        meta.put("audience", "高校学生");

        List<Map<String, Object>> slides = new ArrayList<>();
        slides.add(slide("cover", Map.of("title", kpName, "subtitle", "个性化学习演示")));

        Map<String, Object> concept = extractConceptSection(lesson);
        String summary = String.valueOf(concept.getOrDefault("summary", ""));
        if (summary.isBlank()) {
            summary = String.valueOf(lesson.getOrDefault("summary", "围绕本知识点的核心内容展开"));
        }

        // agenda 取思维导图分支主题，没有则用通用脉络
        List<String> agenda = new ArrayList<>();
        Map<String, Object> mindMap = ResourceAgent.extractResource(resourceAgentData, "mindMap");
        Object branches = mindMap.get("branches");
        if (branches instanceof List) {
            for (Object b : (List<Object>) branches) {
                if (b instanceof Map) {
                    Object topicObj = ((Map<String, Object>) b).get("topic");
                    String topic = topicObj == null ? "" : String.valueOf(topicObj).trim();
                    if (!topic.isEmpty()) agenda.add(topic);
                }
            }
        }
        if (agenda.isEmpty()) {
            agenda = List.of("概念理解", "核心要点", "典型应用", "易错点与小结");
        }
        slides.add(slide("agenda", Map.of("title", "本节脉络", "points", agenda)));

        slides.add(slide("content", Map.of(
                "title", "概念理解",
                "bullets", splitToBullets(summarize(concept.isEmpty()
                        ? summary : String.valueOf(concept.getOrDefault("content", summary)), 400)))));

        // 思维导图分支转为内容页
        if (branches instanceof List) {
            int count = 0;
            for (Object b : (List<Object>) branches) {
                if (count >= 3) break;
                if (!(b instanceof Map)) continue;
                Map<String, Object> branch = (Map<String, Object>) b;
                List<String> points = toStringList(branch.get("points"));
                if (points.isEmpty()) continue;
                slides.add(slide("content", Map.of(
                        "title", String.valueOf(branch.getOrDefault("topic", "要点")),
                        "bullets", points.size() > 5 ? points.subList(0, 5) : points)));
                count++;
            }
        }

        // 实操案例
        Map<String, Object> pc = ResourceAgent.extractResource(resourceAgentData, "practiceCase");
        if (!pc.isEmpty()) {
            slides.add(slide("case", Map.of(
                    "title", String.valueOf(pc.getOrDefault("title", "实操案例")),
                    "scenario", String.valueOf(pc.getOrDefault("scenario", "")),
                    "steps", toStringList(pc.get("steps")))));
        }

        // 小结
        List<String> summaryPoints = new ArrayList<>();
        if (!summary.isBlank()) summaryPoints.add(summary);
        summaryPoints.add("巩固「" + kpName + "」的核心要点，结合练习检验掌握程度");
        slides.add(slide("summary", Map.of("title", "小结", "points", summaryPoints)));

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("meta", meta);
        doc.put("slides", slides);
        return doc;
    }

    private Map<String, Object> slide(String type, Map<String, Object> fields) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("type", type);
        s.putAll(fields);
        return s;
    }

    /** 把一段文字按句号/换行拆成幻灯片要点（最多5条）。 */
    private List<String> splitToBullets(String text) {
        if (text == null || text.isBlank()) {
            return List.of("围绕本知识点展开学习");
        }
        String[] parts = text.split("[。；;\\n]+");
        List<String> bullets = new ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) bullets.add(t.length() > 40 ? t.substring(0, 40) : t);
            if (bullets.size() >= 5) break;
        }
        return bullets.isEmpty() ? List.of(text.length() > 40 ? text.substring(0, 40) : text) : bullets;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeSlidesDoc(Map<String, Object> rawDoc, String kpName, String moduleName) {
        Map<String, Object> source = rawDoc == null ? Collections.emptyMap() : rawDoc;
        Map<String, Object> doc = new LinkedHashMap<>();

        Map<String, Object> rawMeta = source.get("meta") instanceof Map
                ? (Map<String, Object>) source.get("meta")
                : Collections.emptyMap();
        Map<String, Object> meta = new LinkedHashMap<>();
        String title = firstNonBlank(rawMeta.get("title"), kpName, "演示文稿");
        meta.put("title", title);
        meta.put("subtitle", firstNonBlank(rawMeta.get("subtitle"), "个性化学习演示"));
        meta.put("theme", firstNonBlank(rawMeta.get("theme"), "tech-blue"));
        meta.put("audience", firstNonBlank(rawMeta.get("audience"), moduleName + " 学习者"));
        doc.put("meta", meta);

        List<Map<String, Object>> slides = new ArrayList<>();
        Object slidesObj = source.get("slides");
        if (slidesObj instanceof List) {
            for (Object slideObj : (List<?>) slidesObj) {
                if (slideObj instanceof Map) {
                    Map<String, Object> normalized = normalizeSlide((Map<String, Object>) slideObj, title, moduleName);
                    if (!normalized.isEmpty()) {
                        slides.add(normalized);
                    }
                }
            }
        }
        if (slides.isEmpty() || !"cover".equals(slides.get(0).get("type"))) {
            Map<String, Object> cover = new LinkedHashMap<>();
            cover.put("type", "cover");
            cover.put("title", title);
            cover.put("subtitle", firstNonBlank(rawMeta.get("subtitle"), "个性化学习演示"));
            slides.add(0, cover);
        }
        boolean hasSummary = slides.stream().anyMatch(s -> "summary".equals(s.get("type")));
        if (!hasSummary) {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("type", "summary");
            summary.put("title", "小结");
            summary.put("points", List.of("回顾「" + title + "」的核心要点", "结合练习继续巩固掌握程度"));
            summary.put("bullets", summary.get("points"));
            slides.add(summary);
        }
        doc.put("slides", slides);
        doc.put("degraded", Boolean.TRUE.equals(source.get("degraded")));
        doc.put(SLIDES_SNAPSHOT_VERSION_KEY, SLIDES_SNAPSHOT_VERSION);
        return doc;
    }

    private Map<String, Object> normalizeSlide(Map<String, Object> raw, String kpName, String moduleName) {
        Map<String, Object> slide = new LinkedHashMap<>();
        String type = firstNonBlank(raw.get("type"), "content");
        if (!PresentationAgent.SLIDE_TYPES.contains(type)) {
            type = "content";
        }
        slide.put("type", type);

        String title = firstNonBlank(raw.get("title"), defaultSlideTitle(type, kpName));
        slide.put("title", title);
        switch (type) {
            case "cover":
                slide.put("subtitle", firstNonBlank(raw.get("subtitle"), moduleName + " 个性化学习演示"));
                break;
            case "agenda": {
                List<String> points = normalizeSlideItems(List.of("课程目标", "核心要点", "案例应用", "小结巩固"),
                        raw.get("points"), raw.get("bullets"), raw.get("items"), raw.get("outline"));
                slide.put("points", points);
                slide.put("bullets", points);
                break;
            }
            case "content": {
                List<String> bullets = normalizeSlideItems(List.of("围绕本知识点展开学习"),
                        raw.get("bullets"), raw.get("points"), raw.get("items"), raw.get("content"));
                slide.put("bullets", bullets);
                slide.put("points", bullets);
                putIfNotBlank(slide, "note", firstNonBlank(raw.get("note")));
                break;
            }
            case "case": {
                slide.put("scenario", firstNonBlank(raw.get("scenario"), raw.get("content"), "结合「" + kpName + "」完成一个应用案例"));
                List<String> steps = normalizeSlideItems(List.of("明确任务目标", "应用核心方法", "检查结果并总结"),
                        raw.get("steps"), raw.get("points"), raw.get("bullets"), raw.get("items"));
                slide.put("steps", steps);
                break;
            }
            case "formula":
                slide.put("latex", stripLatexDelimiters(firstNonBlank(raw.get("latex"), raw.get("formula"), "")));
                slide.put("explain", firstNonBlank(raw.get("explain"), raw.get("description"), "理解公式中各变量含义与适用条件"));
                break;
            case "code":
                slide.put("lang", firstNonBlank(raw.get("lang"), "text"));
                slide.put("code", firstNonBlank(raw.get("code"), raw.get("snippet"), ""));
                slide.put("explain", firstNonBlank(raw.get("explain"), raw.get("description"), ""));
                break;
            case "summary":
            default: {
                List<String> points = normalizeSlideItems(List.of("回顾核心概念", "整理易错点", "通过练习检验掌握"),
                        raw.get("points"), raw.get("bullets"), raw.get("items"), raw.get("content"));
                slide.put("points", points);
                slide.put("bullets", points);
                break;
            }
        }
        return slide;
    }

    private String defaultSlideTitle(String type, String kpName) {
        switch (type) {
            case "cover": return kpName;
            case "agenda": return "本节脉络";
            case "case": return "实操案例";
            case "formula": return "关键公式";
            case "code": return "代码示例";
            case "summary": return "小结";
            default: return "核心要点";
        }
    }

    private void putIfNotBlank(Map<String, Object> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }

    private List<String> normalizeSlideItems(List<String> fallback, Object... values) {
        List<String> items = new ArrayList<>();
        for (Object value : values) {
            collectSlideItems(value, items);
            if (!items.isEmpty()) {
                break;
            }
        }
        if (items.isEmpty()) {
            items.addAll(fallback);
        }
        return items.stream()
                .map(s -> s.length() > 60 ? s.substring(0, 60) : s)
                .limit(6)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private void collectSlideItems(Object value, List<String> out) {
        if (value == null) {
            return;
        }
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                collectSlideItems(item, out);
            }
            return;
        }
        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            String text = firstNonBlank(map.get("title"), map.get("text"), map.get("content"),
                    map.get("point"), map.get("step"), map.get("name"));
            if (!text.isBlank()) {
                out.add(text);
            }
            return;
        }
        String text = String.valueOf(value).trim();
        if (text.isBlank() || "null".equals(text)) {
            return;
        }
        for (String part : text.split("[\\r\\n。；;]+")) {
            String item = part.replaceFirst("^[\\-•·*\\d.、)）\\s]+", "").trim();
            if (!item.isBlank()) {
                out.add(item);
            }
        }
    }

    private String stripLatexDelimiters(String value) {
        String text = firstNonBlank(value);
        while (text.startsWith("$")) {
            text = text.substring(1).trim();
        }
        while (text.endsWith("$")) {
            text = text.substring(0, text.length() - 1).trim();
        }
        return text;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findSlidesSnapshot(LearningPath path, String nodeId) {
        Map<String, Object> snapshots = parseJson(path.getLessonSnapshots(),
                new TypeReference<Map<String, Object>>() {}, Collections.emptyMap());
        Object lessonObj = snapshots.get(nodeId);
        if (!(lessonObj instanceof Map)) {
            return Collections.emptyMap();
        }
        Object slidesObj = ((Map<String, Object>) lessonObj).get("slidesDoc");
        if (slidesObj instanceof Map && ((Map<?, ?>) slidesObj).get("slides") instanceof List) {
            Map<String, Object> slidesDoc = new LinkedHashMap<>((Map<String, Object>) slidesObj);
            Object versionObj = slidesDoc.get(SLIDES_SNAPSHOT_VERSION_KEY);
            int version = versionObj instanceof Number ? ((Number) versionObj).intValue() : 0;
            if (version != SLIDES_SNAPSHOT_VERSION) {
                log.warn("演示文稿快照版本不匹配（快照v{} 期望v{}），视为未命中并重新生成: nodeId={}",
                        version, SLIDES_SNAPSHOT_VERSION, nodeId);
                return Collections.emptyMap();
            }
            Map<String, Object> node = findPathNode(path, nodeId);
            String kpName = firstNonBlank(node.get("knowledgePointName"), node.get("title"), "演示文稿");
            String moduleName = firstNonBlank(node.get("module"), "高校课程");
            return normalizeSlidesDoc(slidesDoc, kpName, moduleName);
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private void saveSlidesSnapshot(LearningPath path, String nodeId, Map<String, Object> slidesDoc) {
        Map<String, Object> snapshots = new LinkedHashMap<>(parseJson(path.getLessonSnapshots(),
                new TypeReference<Map<String, Object>>() {}, Collections.emptyMap()));
        Map<String, Object> lessonSnapshot = new LinkedHashMap<>();
        Object existing = snapshots.get(nodeId);
        if (existing instanceof Map) {
            lessonSnapshot.putAll((Map<String, Object>) existing);
        }
        lessonSnapshot.put("slidesDoc", slidesDoc);
        snapshots.put(nodeId, lessonSnapshot);
        try {
            path.setLessonSnapshots(objectMapper.writeValueAsString(snapshots));
            learningPathRepository.save(path);
        } catch (Exception e) {
            log.warn("保存演示文稿快照失败, nodeId={}: {}", nodeId, e.getMessage());
        }
    }

    /**
     * 导出该节点演示文稿为 .pptx 文件（先复用 slides 生成逻辑拿到大纲，再用 POI 渲染）。
     */
    public PptxFile exportLessonSlidesPptx(Long studentId, Long pathId, String nodeId) {
        Map<String, Object> slidesDoc = findLessonSlides(studentId, pathId, nodeId);
        byte[] content = pptxRenderer.render(slidesDoc);
        String title = "演示文稿";
        Object meta = slidesDoc.get("meta");
        if (meta instanceof Map && ((Map<?, ?>) meta).get("title") != null) {
            title = String.valueOf(((Map<?, ?>) meta).get("title"));
        }
        String safeTitle = title.replaceAll("[\\\\/:*?\"<>|]", "").trim();
        if (safeTitle.isEmpty()) safeTitle = "演示文稿";
        return new PptxFile(safeTitle + ".pptx", content);
    }

    /** .pptx 下载封装：文件名 + 字节内容。 */
    public static class PptxFile {
        private final String filename;
        private final byte[] content;

        public PptxFile(String filename, byte[] content) {
            this.filename = filename;
            this.content = content;
        }

        public String getFilename() {
            return filename;
        }

        public byte[] getContent() {
            return content;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> submitExercise(Long studentId, Long pathId, String nodeId,
                                              String exerciseId, String answer,
                                              String solvingSteps, Integer timeSpentSeconds) {
        LearningPath path = learningPathRepository.findById(pathId)
                .orElseThrow(() -> new BusinessException(404, "学习路径不存在"));

        if (!path.getStudentId().equals(studentId)) {
            throw new BusinessException(403, "无权访问该学习路径");
        }

        // 解析节点找到对应练习题的正确答案
        List<Map<String, Object>> nodes = parseJson(path.getNodes(),
                new TypeReference<List<Map<String, Object>>>() {}, Collections.emptyList());

        Map<String, Object> targetNode = nodes.stream()
                .filter(n -> nodeId.equals(n.get("nodeId")))
                .findFirst()
                .orElse(Collections.emptyMap());

        String kpId = (String) targetNode.getOrDefault("knowledgePointId", nodeId);
        String kpName = (String) targetNode.getOrDefault("knowledgePointName", resolveKpName(kpId));
        double currentMastery = 0.5;
        Object masteryObj = targetNode.get("currentMastery");
        if (masteryObj instanceof Number) {
            currentMastery = ((Number) masteryObj).doubleValue();
        }

        String correctAnswer = findCorrectAnswer(path, nodeId, exerciseId);
        boolean isCorrect = answer != null && answer.trim().equalsIgnoreCase(correctAnswer.trim());

        String moduleName = Optional.ofNullable(knowledgeGraphService.getNode(kpId))
                .map(KnowledgeNode::getModule)
                .orElse("高校课程");
        double newMastery = masteryUpdateService.updateFromAnswer(
                studentId, kpId, moduleName, isCorrect, "practice");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("exerciseId", exerciseId);
        result.put("isCorrect", isCorrect);
        result.put("correct", isCorrect);
        result.put("correctAnswer", correctAnswer);
        result.put("studentAnswer", answer);
        result.put("score", isCorrect ? 20 : 0);
        result.put("explanation", findExerciseExplanation(path, nodeId, exerciseId));

        if (!isCorrect) {
            Map<String, Object> errorAnalysis = new LinkedHashMap<>();
            KnowledgeNode kpNode = knowledgeGraphService.getNode(kpId);
            List<String> errorTypes = kpNode != null && kpNode.getErrorTypes() != null ? kpNode.getErrorTypes() : Collections.emptyList();
            errorAnalysis.put("errorType", !errorTypes.isEmpty() ? errorTypes.get(0) : "理解偏差");
            errorAnalysis.put("knowledgePoint", kpName);
            errorAnalysis.put("suggestion", "建议重新学习" + kpName + "相关内容，重点关注核心概念和公式推导");
            result.put("errorAnalysis", errorAnalysis);
        }

        Map<String, Object> masteryUpdate = new LinkedHashMap<>();
        masteryUpdate.put("before", currentMastery);
        masteryUpdate.put("after", newMastery);
        masteryUpdate.put("delta", newMastery - currentMastery);
        result.put("masteryUpdate", masteryUpdate);

        // 更新节点掌握度
        updateNodeMastery(path, nodes, nodeId, newMastery);

        if (isCorrect) {
            result.put("encouragement", "做得好！继续保持这个势头！");
        } else {
            result.put("encouragement", "别灰心，错误是学习的一部分。让我们回顾一下这个知识点，相信你下次一定能做对！");
        }

        result.put("timeSpentSeconds", timeSpentSeconds);

        path.setLastStudyAt(LocalDateTime.now());
        learningPathRepository.save(path);

        return result;
    }

    /**
     * 提交检查点测试：调用 EvaluationAgent 评估掌握度
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> submitCheckpoint(Long studentId, Long pathId, String nodeId,
                                                List<Map<String, Object>> answers,
                                                Integer totalTimeSeconds) {
        LearningPath path = learningPathRepository.findById(pathId)
                .orElseThrow(() -> new BusinessException(404, "学习路径不存在"));

        if (!path.getStudentId().equals(studentId)) {
            throw new BusinessException(403, "无权访问该学习路径");
        }

        List<Map<String, Object>> nodes = parseJson(path.getNodes(),
                new TypeReference<List<Map<String, Object>>>() {}, Collections.emptyList());

        Map<String, Object> targetNode = nodes.stream()
                .filter(n -> nodeId.equals(n.get("nodeId")))
                .findFirst()
                .orElse(Collections.emptyMap());

        String kpId = (String) targetNode.getOrDefault("knowledgePointId", nodeId);
        String kpName = (String) targetNode.getOrDefault("knowledgePointName", resolveKpName(kpId));
        double currentMastery = 0.5;
        Object mObj = targetNode.get("currentMastery");
        if (mObj instanceof Number) {
            currentMastery = ((Number) mObj).doubleValue();
        }

        KnowledgeNode kpNode = knowledgeGraphService.getNode(kpId);
        String moduleName = kpNode != null ? kpNode.getModule() : "高校课程";

        // 调用 EvaluationAgent 生成检测题并评估
        AgentContext context = AgentContext.builder()
                .studentId(studentId)
                .module(moduleName)
                .knowledgeMastery(new HashMap<>())
                .sessionData(new HashMap<>())
                .build();
        context.updateMastery(kpId, currentMastery);
        context.putSessionData("knowledgePointId", kpId);
        context.putSessionData("knowledgePointName", kpName);
        context.putSessionData("masteryLevel", currentMastery);
        context.putSessionData("action", "evaluate");

        List<Map<String, Object>> checkpointQuestions = buildCheckpointQuestionsFromSnapshots(path, nodeId, answers);
        Map<String, String> checkpointAnswers = buildCheckpointAnswers(checkpointQuestions, answers);
        Map<String, Boolean> localCorrectness = evaluateCheckpointLocally(checkpointQuestions, checkpointAnswers);

        context.putSessionData("checkpointQuestions", checkpointQuestions);
        context.putSessionData("checkpointAnswers", checkpointAnswers);
        context.putSessionData("serverSnapshotUsed", true);

        AgentResponse evalResponse = evaluationAgent.execute(context);

        // 画像驱动：检查点通过线按基础水平自适应（基础弱 0.5 / 中等 0.6 / 较强 0.7）
        StudentProfile cpProfile = studentProfileRepository.findByStudentId(studentId).orElse(null);
        double passThreshold = thresholdByFoundation(cpProfile != null ? cpProfile.getFoundationLevel() : null);

        // 从评估结果中获取掌握度和通过状态
        double finalMastery = currentMastery;
        boolean passed = false;
        int correctCount = (int) localCorrectness.values().stream().filter(Boolean.TRUE::equals).count();

        List<Map<String, Object>> questionResults = new ArrayList<>();

        if (evalResponse.isSuccess()) {
            Map<String, Object> evalData = evalResponse.getData();
            Object fmObj = evalData.get("finalMastery");
            if (fmObj instanceof Number) {
                finalMastery = ((Number) fmObj).doubleValue();
            }
            // 以个性化阈值为准复核通过状态，保证阈值真正生效
            passed = finalMastery >= passThreshold;

            // 从 detailedResults 构建题目级结果
            Object drObj = evalData.get("detailedResults");
            if (drObj instanceof List) {
                List<Map<String, Object>> detailedResults = (List<Map<String, Object>>) drObj;
                for (Map<String, Object> dr : detailedResults) {
                    String qId = String.valueOf(dr.getOrDefault("questionId", ""));
                    Map<String, Object> qr = new LinkedHashMap<>();
                    qr.put("questionId", qId);
                    boolean localCorrect = Boolean.TRUE.equals(localCorrectness.get(qId));
                    qr.put("isCorrect", localCorrect);
                    qr.put("analysis", dr.getOrDefault("analysis", ""));
                    if (!localCorrect) {
                        qr.put("errorType", dr.getOrDefault("errorType", ""));
                    }
                    questionResults.add(qr);
                }
            }
        } else {
            log.warn("EvaluationAgent 失败，使用服务端快照本地评估: {}", evalResponse.getMessage());
        }

        if (questionResults.isEmpty()) {
            questionResults = buildLocalCheckpointResults(checkpointQuestions, checkpointAnswers, localCorrectness);
        }

        if (!evalResponse.isSuccess()) {
            int total = checkpointQuestions.size() > 0 ? checkpointQuestions.size() : 1;
            finalMastery = (double) correctCount / total;
            passed = finalMastery >= passThreshold;
        }

        // 通过闸门：检查点不仅看融合历史的掌握度，还必须保证本次答对率达标，
        // 否则会出现“本次只答对少数题（如 33 分）却因历史掌握度高而通过”的不合理结果。
        int gateTotal = checkpointQuestions.size() > 0 ? checkpointQuestions.size() : 1;
        double currentScoreRatio = (double) correctCount / gateTotal;
        if (passed && currentScoreRatio < passThreshold) {
            passed = false;
            log.info("检查点本次答对率 {} 低于阈值 {}，覆盖掌握度通过判定为未通过 (studentId={}, nodeId={})",
                    currentScoreRatio, passThreshold, studentId, nodeId);
        }

        // 更新节点和路径状态
        masteryUpdateService.recordKnowledgePointMastery(
                studentId, kpId, moduleName, finalMastery, "checkpoint");

        String remediationNodeId = null;
        Map<String, Object> remediationPlan = Collections.emptyMap();
        if (passed) {
            for (Map<String, Object> node : nodes) {
                if (nodeId.equals(node.get("nodeId"))) {
                    node.put("status", "completed");
                    node.put("currentMastery", finalMastery);
                    break;
                }
            }
            completeRemediatedSourceNode(nodes, targetNode, finalMastery);
            unlockNextLockedNode(nodes, nodeId);
            appendContinuationNodesAfterPass(path, nodes, targetNode);

            try {
                path.setNodes(objectMapper.writeValueAsString(nodes));
                path.setTracingPath(objectMapper.writeValueAsString(buildTracingPathEdges(nodes)));
            } catch (Exception ignored) {}

            int completed = (int) nodes.stream().filter(n -> "completed".equals(n.get("status"))).count();
            path.setTotalNodes(nodes.size());
            path.setCompletedNodes(completed);
            path.setProgress(toRatioProgress(completed, nodes.size()));
            path.setCurrentNodeId(findNextActionableNodeId(nodes));

            if (completed == nodes.size()) {
                path.setStatus("completed");
                path.setCompletedAt(LocalDateTime.now());
                path.setCurrentNodeId(null);
            }
        } else {
            markNodeFailed(nodes, nodeId, finalMastery);
            remediationPlan = buildRemediationPlan(nodeId, kpId, kpName, questionResults);
            remediationNodeId = upsertRemediationNode(nodes, nodeId, remediationPlan);
            try {
                path.setNodes(objectMapper.writeValueAsString(nodes));
                path.setTotalNodes(nodes.size());
            } catch (Exception ignored) {}
            path.setCurrentNodeId(remediationNodeId);
        }

        path.setLastStudyAt(LocalDateTime.now());
        learningPathRepository.save(path);

        int totalQuestions = checkpointQuestions.size() > 0 ? checkpointQuestions.size() : 1;
        double score = (double) correctCount / totalQuestions * 100;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pathId", pathId);
        result.put("nodeId", nodeId);
        result.put("knowledgePointId", kpId);
        result.put("knowledgePointName", kpName);
        result.put("passed", passed);
        result.put("score", score);
        result.put("masteryAfter", finalMastery);
        result.put("masteryThreshold", passThreshold);
        result.put("totalQuestions", totalQuestions);
        result.put("correctCount", correctCount);
        result.put("results", questionResults);
        result.put("serverSnapshotUsed", true);
        result.put("remediationPlan", remediationPlan);
        result.put("currentNodeId", path.getCurrentNodeId());

        Map<String, Object> nextAction = new LinkedHashMap<>();
        if (passed) {
            String nextNodeId = path.getCurrentNodeId();
            if (nextNodeId != null) {
                nextAction.put("type", "next_node");
                nextAction.put("nextNodeId", nextNodeId);
                nextAction.put("nextKnowledgePointName", resolveKpNameFromNodes(nodes, nextNodeId));
                nextAction.put("message", "恭喜通过检查点！下一个知识节点已解锁。");
            } else {
                nextAction.put("type", "path_complete");
                nextAction.put("message", "恭喜！学习路径全部完成！");
            }
        } else {
            nextAction.put("type", "remediation");
            nextAction.put("nextNodeId", remediationNodeId);
            nextAction.put("nextKnowledgePointName", kpName);
            nextAction.put("message", "未通过检查点，已生成补救节点，请先完成针对性补练。");
            nextAction.put("suggestedActions", remediationPlan.getOrDefault("actions",
                    Arrays.asList("重新学习本节课程内容", "完成补救练习", "再次挑战检查点测试")));
        }
        result.put("nextAction", nextAction);
        if (nextAction.containsKey("nextNodeId")) {
            result.put("nextNodeId", nextAction.get("nextNodeId"));
        }
        if (!passed && remediationNodeId != null && !remediationNodeId.isBlank()) {
            result.put("remediationNodeId", remediationNodeId);
        }
        result.put("pathProgress", toProgressPercent(path.getProgress()));

        return result;
    }

    // ================= Private helper methods =================

    /**
     * 从 PlanningAgent 返回结果构建节点列表
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildNodesFromPlanningResult(Map<String, Object> planData,
                                                                    List<KnowledgeNode> sortedNodes,
                                                                    int dailyMinutes) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        Object lpObj = planData.get("learningPath");
        if (!(lpObj instanceof List)) {
            return nodes;
        }

        List<Map<String, Object>> learningPathItems = (List<Map<String, Object>>) lpObj;
        Map<String, KnowledgeNode> sortedNodeMap = sortedNodes.stream()
                .collect(Collectors.toMap(KnowledgeNode::getId, n -> n, (a, b) -> a, LinkedHashMap::new));
        Map<String, Map<String, Object>> plannedById = new LinkedHashMap<>();
        for (Map<String, Object> item : learningPathItems) {
            String nodeId = firstNonBlank(item.get("nodeId"), item.get("knowledgePointId"));
            if (!nodeId.isBlank() && sortedNodeMap.containsKey(nodeId)) {
                plannedById.putIfAbsent(nodeId, item);
            }
        }

        int order = 1;
        for (KnowledgeNode sortedNode : sortedNodes) {
            String nodeId = sortedNode.getId();
            Map<String, Object> item = plannedById.getOrDefault(nodeId, Collections.emptyMap());

            int estimatedMinutes = Math.max(15, dailyMinutes / 2);
            Object emObj = item.get("estimatedMinutes");
            if (emObj instanceof Number) {
                estimatedMinutes = ((Number) emObj).intValue();
            }

            String priority = String.valueOf(item.getOrDefault("priority",
                    order <= 2 ? "high" : "medium"));
            KnowledgeNode kpNode = sortedNode;

            Map<String, Object> node = new LinkedHashMap<>();
            node.put("nodeId", "node_" + order);
            node.put("order", order);
            node.put("knowledgePointId", nodeId);
            node.put("knowledgePointName", kpNode != null ? kpNode.getName() : nodeId);
            node.put("title", kpNode != null ? kpNode.getName() : nodeId);
            node.put("knowledgePoint", kpNode != null ? kpNode.getName() : nodeId);
            node.put("currentMastery", 0.0);
            node.put("teachingStrategy", determineStrategy(priority));
            node.put("estimatedMinutes", estimatedMinutes);
            node.put("estimatedTime", estimatedMinutes);
            node.put("type", "lesson");
            node.put("status", order == 1 ? "unlocked" : "locked");
            node.put("reason", item.getOrDefault("reason", ""));

            // 学习路径内按线性顺序推进，知识图谱原始前置只作为元数据展示。
            node.put("prerequisites", order > 1 ? Arrays.asList("node_" + (order - 1)) : Collections.emptyList());
            if (kpNode.getPrerequisites() != null) {
                node.put("knowledgePrerequisites", kpNode.getPrerequisites());
            }
            enrichNodeWithKnowledgeMetadata(node, kpNode);

            nodes.add(node);
            order++;
        }
        return nodes;
    }

    /**
     * 按基础水平返回学习节奏系数：基础弱给更充裕时间（×1.3），较强更紧凑（×0.8），中等不变。
     * 兼容多种中文表述。
     */
    private double paceFactorByFoundation(String foundationLevel) {
        if (foundationLevel == null || foundationLevel.isBlank()) {
            return 1.0;
        }
        String f = foundationLevel.trim();
        if (f.contains("较强") || f.contains("扎实") || f.contains("进阶") || f.contains("强")) {
            return 0.8;
        }
        if (f.contains("中")) {
            return 1.0;
        }
        if (f.contains("基础") || f.contains("弱") || f.contains("入门") || f.contains("差")) {
            return 1.3;
        }
        return 1.0;
    }

    /** 取 Object 的字符串值，null/"null"/空白返回空串。 */
    private String stringOrEmpty(Object o) {
        if (o == null) return "";
        String s = String.valueOf(o).trim();
        return ("null".equals(s)) ? "" : s;
    }

    /** 按系数缩放每个节点的预计时长（estimatedMinutes/estimatedTime），下限 10 分钟。 */
    private void applyPaceToNodes(List<Map<String, Object>> nodes, double factor) {
        if (nodes == null || Math.abs(factor - 1.0) < 1e-6) {
            return;
        }
        for (Map<String, Object> node : nodes) {
            Object emObj = node.get("estimatedMinutes");
            if (emObj instanceof Number) {
                int scaled = Math.max(10, (int) Math.ceil(((Number) emObj).intValue() * factor));
                node.put("estimatedMinutes", scaled);
                node.put("estimatedTime", scaled);
            }
        }
    }

    /**
     * 按基础水平返回检查点通过阈值：基础弱 0.5、中等 0.6、较强 0.7。
     */
    private double thresholdByFoundation(String foundationLevel) {
        if (foundationLevel == null || foundationLevel.isBlank()) {
            return 0.6;
        }
        String f = foundationLevel.trim();
        if (f.contains("较强") || f.contains("扎实") || f.contains("进阶") || f.contains("强")) {
            return 0.7;
        }
        if (f.contains("中")) {
            return 0.6;
        }
        if (f.contains("基础") || f.contains("弱") || f.contains("入门") || f.contains("差")) {
            return 0.5;
        }
        return 0.6;
    }

    /**
     * Fallback: 使用知识图谱拓扑序直接构建节点
     */
    private List<Map<String, Object>> buildNodesFromGraphFallback(List<KnowledgeNode> sortedNodes, int dailyMinutes) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        int order = 1;
        for (KnowledgeNode kpNode : sortedNodes) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("nodeId", "node_" + order);
            node.put("order", order);
            node.put("knowledgePointId", kpNode.getId());
            node.put("knowledgePointName", kpNode.getName());
            node.put("title", kpNode.getName());
            node.put("knowledgePoint", kpNode.getName());
            node.put("currentMastery", 0.0);
            node.put("teachingStrategy", "basic_consolidation");
            node.put("estimatedMinutes", Math.max(15, dailyMinutes / 2));
            node.put("estimatedTime", Math.max(15, dailyMinutes / 2));
            node.put("type", "lesson");
            node.put("status", order == 1 ? "unlocked" : "locked");
            node.put("prerequisites", order > 1 ? Arrays.asList("node_" + (order - 1)) : Collections.emptyList());
            enrichNodeWithKnowledgeMetadata(node, kpNode);
            nodes.add(node);
            order++;
        }
        return nodes;
    }

    private List<Map<String, Object>> buildNodesFromTracingFallback(TracingResult tracingResult,
                                                                    String targetKnowledgePointId,
                                                                    List<String> rootCauseIds,
                                                                    Map<String, Object> pointIndex,
                                                                    String moduleName,
                                                                    int dailyMinutes) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        Set<String> orderedPointIds = new LinkedHashSet<>();

        List<Map<String, Object>> suggestedPath = parseJson(tracingResult.getSuggestedLearningPath(),
                new TypeReference<List<Map<String, Object>>>() {}, Collections.emptyList());
        for (Map<String, Object> step : suggestedPath) {
            String kpId = firstNonBlank(step.get("knowledgePointId"));
            if (!kpId.isBlank() && !"review".equals(kpId)) {
                orderedPointIds.add(kpId);
                pointIndex.putIfAbsent(kpId, step);
            }
        }

        for (String rootId : rootCauseIds) {
            if (rootId != null && !rootId.isBlank() && !"null".equals(rootId) && !"review".equals(rootId)) {
                orderedPointIds.add(rootId);
            }
        }
        if (targetKnowledgePointId != null && !targetKnowledgePointId.isBlank()
                && !"review".equals(targetKnowledgePointId)) {
            orderedPointIds.add(targetKnowledgePointId);
        }
        expandFallbackPathIfTooShort(orderedPointIds, targetKnowledgePointId, moduleName);

        if (orderedPointIds.isEmpty()) {
            return nodes;
        }

        int order = 1;
        int defaultMinutes = Math.max(20, dailyMinutes);
        for (String kpId : orderedPointIds) {
            Map<String, Object> indexed = pointIndex.get(kpId) instanceof Map
                    ? (Map<String, Object>) pointIndex.get(kpId)
                    : Collections.emptyMap();
            String kpName = resolveKpName(kpId, pointIndex);
            String phase = firstNonBlank(indexed.get("phase"),
                    rootCauseIds.contains(kpId) ? "基础补强" : "专项突破");

            Map<String, Object> node = new LinkedHashMap<>();
            node.put("nodeId", "node_" + order);
            node.put("order", order);
            node.put("knowledgePointId", kpId);
            node.put("knowledgePointName", kpName);
            node.put("title", kpName);
            node.put("knowledgePoint", kpName);
            node.put("module", firstNonBlank(indexed.get("module"), moduleName, "高校课程"));
            node.put("currentMastery", readNumber(indexed.get("mastery"), 0.0));
            node.put("teachingStrategy", rootCauseIds.contains(kpId) ? "basic_consolidation" : "strengthening");
            node.put("estimatedMinutes", readEstimatedMinutes(indexed.get("estimatedTime"), defaultMinutes));
            node.put("estimatedTime", readEstimatedMinutes(indexed.get("estimatedTime"), defaultMinutes));
            node.put("type", "lesson");
            node.put("status", order == 1 ? "unlocked" : "locked");
            node.put("prerequisites", order > 1 ? Arrays.asList("node_" + (order - 1)) : Collections.emptyList());
            node.put("reason", "来自「" + node.get("module") + "」诊断溯因结果：" + phase);
            node.put("customKnowledgePoint", true);
            nodes.add(node);
            order++;
        }
        return nodes;
    }

    private void expandFallbackPathIfTooShort(Set<String> orderedPointIds,
                                              String targetKnowledgePointId,
                                              String moduleName) {
        if (orderedPointIds.size() >= 3) {
            return;
        }
        appendContinuationKnowledgePoints(orderedPointIds, targetKnowledgePointId, moduleName, 3);
    }

    private void appendContinuationNodesAfterPass(LearningPath path,
                                                  List<Map<String, Object>> nodes,
                                                  Map<String, Object> completedNode) {
        if (findNextActionableNodeId(nodes) != null) {
            return;
        }
        Set<String> existingPointIds = nodes.stream()
                .map(node -> firstNonBlank(node.get("knowledgePointId")))
                .filter(id -> !id.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        String currentKpId = firstNonBlank(completedNode.get("knowledgePointId"), path.getTargetKnowledgePointId());
        String moduleName = firstNonBlank(completedNode.get("module"), resolvePathModule(path, nodes));
        int before = existingPointIds.size();
        appendContinuationKnowledgePoints(existingPointIds, currentKpId, moduleName, before + 1);
        if (existingPointIds.size() <= before) {
            return;
        }

        int firstNewOrder = nodes.size() + 1;
        int order = firstNewOrder;
        int defaultMinutes = Math.max(20, path.getTotalEstimatedMinutes() != null && path.getTotalNodes() != null
                && path.getTotalNodes() > 0 ? path.getTotalEstimatedMinutes() / path.getTotalNodes() : 20);
        String previousNodeId = firstNonBlank(completedNode.get("nodeId"));
        for (String kpId : existingPointIds) {
            if (nodes.stream().anyMatch(node -> kpId.equals(firstNonBlank(node.get("knowledgePointId"))))) {
                continue;
            }
            KnowledgeNode kpNode = knowledgeGraphService.getNode(kpId);
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("nodeId", "node_" + order);
            node.put("order", order);
            node.put("knowledgePointId", kpId);
            node.put("knowledgePointName", kpNode != null ? kpNode.getName() : kpId);
            node.put("title", kpNode != null ? kpNode.getName() : kpId);
            node.put("knowledgePoint", kpNode != null ? kpNode.getName() : kpId);
            node.put("module", kpNode != null ? kpNode.getModule() : moduleName);
            node.put("currentMastery", 0.0);
            node.put("teachingStrategy", "strengthening");
            node.put("estimatedMinutes", kpNode != null && kpNode.getEstimatedMinutes() != null
                    ? kpNode.getEstimatedMinutes() : defaultMinutes);
            node.put("estimatedTime", node.get("estimatedMinutes"));
            node.put("type", "lesson");
            node.put("status", order == firstNewOrder ? "unlocked" : "locked");
            node.put("prerequisites", !previousNodeId.isBlank()
                    ? Collections.singletonList(previousNodeId) : Collections.emptyList());
            node.put("reason", "当前节点通过后自动延伸的后续学习节点");
            enrichNodeWithKnowledgeMetadata(node, kpNode);
            nodes.add(node);
            previousNodeId = String.valueOf(node.get("nodeId"));
            order++;
        }
    }

    private void appendContinuationKnowledgePoints(Set<String> pointIds,
                                                   String startKpId,
                                                   String moduleName,
                                                   int desiredSize) {
        if (pointIds.size() >= desiredSize) {
            return;
        }
        Queue<String> queue = new LinkedList<>();
        if (startKpId != null && !startKpId.isBlank()) {
            queue.add(startKpId);
        }
        while (!queue.isEmpty() && pointIds.size() < desiredSize) {
            String current = queue.poll();
            for (KnowledgeNode dependent : knowledgeGraphService.getDependents(current)) {
                if (pointIds.add(dependent.getId())) {
                    queue.add(dependent.getId());
                    if (pointIds.size() >= desiredSize) {
                        return;
                    }
                }
            }
        }

        List<KnowledgeNode> moduleNodes = knowledgeGraphService.getNodesByModule(moduleName);
        for (KnowledgeNode node : moduleNodes) {
            pointIds.add(node.getId());
            if (pointIds.size() >= desiredSize) {
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildTracingPointNameIndex(TracingResult tracingResult) {
        Map<String, Object> index = new LinkedHashMap<>();

        List<Map<String, Object>> suggestedPath = parseJson(tracingResult.getSuggestedLearningPath(),
                new TypeReference<List<Map<String, Object>>>() {}, Collections.emptyList());
        for (Map<String, Object> step : suggestedPath) {
            putPointIndex(index, step.get("knowledgePointId"), step);
        }

        List<Map<String, Object>> tracingResults = parseJson(tracingResult.getTracingResults(),
                new TypeReference<List<Map<String, Object>>>() {}, Collections.emptyList());
        for (Map<String, Object> tr : tracingResults) {
            String targetId = firstNonBlank(tr.get("targetPointId"));
            if (!targetId.isBlank()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("knowledgePointId", targetId);
                item.put("knowledgePointName", firstNonBlank(tr.get("targetPointName"), targetId));
                putPointIndex(index, targetId, item);
            }
            Object tracingPathObj = tr.get("tracingPath");
            if (tracingPathObj instanceof List) {
                for (Object nodeObj : (List<?>) tracingPathObj) {
                    if (nodeObj instanceof Map) {
                        Map<String, Object> node = (Map<String, Object>) nodeObj;
                        putPointIndex(index, node.get("knowledgePointId"), node);
                    }
                }
            }
        }

        List<Map<String, Object>> rootCauses = parseJson(tracingResult.getMergedRootCauses(),
                new TypeReference<List<Map<String, Object>>>() {}, Collections.emptyList());
        for (Map<String, Object> rc : rootCauses) {
            String nodeId = extractKnowledgePointId(rc);
            if (!nodeId.isBlank()) {
                Map<String, Object> item = new LinkedHashMap<>(rc);
                item.put("knowledgePointId", nodeId);
                item.put("knowledgePointName", firstNonBlank(rc.get("knowledgePointName"), rc.get("nodeName"), nodeId));
                putPointIndex(index, nodeId, item);
            }
        }
        return index;
    }

    private void putPointIndex(Map<String, Object> index, Object rawId, Map<String, Object> data) {
        String id = firstNonBlank(rawId);
        if (id.isBlank() || "review".equals(id) || "null".equals(id) || "undefined".equals(id)) {
            return;
        }
        index.putIfAbsent(id, data);
    }

    private String resolveModuleFromTracing(TracingResult tracingResult) {
        String diagnosticId = tracingResult.getDiagnosticId();
        if (diagnosticId != null && !diagnosticId.isBlank()) {
            Optional<DiagnosticSession> session = diagnosticSessionRepository.findByDiagnosticId(diagnosticId);
            if (session.isPresent() && session.get().getModule() != null && !session.get().getModule().isBlank()) {
                return session.get().getModule();
            }
        }
        return "高校课程";
    }

    private int readEstimatedMinutes(Object value, int defaultMinutes) {
        if (value instanceof Number) {
            return Math.max(10, ((Number) value).intValue());
        }
        String text = firstNonBlank(value);
        if (!text.isBlank()) {
            String digits = text.replaceAll("[^0-9]", "");
            if (!digits.isBlank()) {
                try {
                    return Math.max(10, Integer.parseInt(digits));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return defaultMinutes;
    }

    private double readNumber(Object value, double defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        String text = firstNonBlank(value);
        if (!text.isBlank()) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private void enrichNodeWithKnowledgeMetadata(Map<String, Object> node, KnowledgeNode kpNode) {
        if (kpNode == null) {
            return;
        }
        if (kpNode.getEstimatedMinutes() != null && kpNode.getEstimatedMinutes() > 0) {
            node.put("estimatedMinutes", kpNode.getEstimatedMinutes());
            node.put("estimatedTime", kpNode.getEstimatedMinutes());
        }
        node.put("examWeight", kpNode.getExamWeight());
        node.put("difficulty", kpNode.getDifficulty());
        node.put("commonErrors", collectCommonErrors(kpNode));
        if (kpNode.getTeachingTips() != null && !kpNode.getTeachingTips().isBlank()) {
            node.put("teachingTips", kpNode.getTeachingTips());
        }
    }

    private List<String> collectCommonErrors(KnowledgeNode kpNode) {
        List<String> errors = new ArrayList<>();
        if (kpNode.getCommonErrors() != null) {
            errors.addAll(kpNode.getCommonErrors());
        }
        if (errors.isEmpty() && kpNode.getErrorTypes() != null) {
            errors.addAll(kpNode.getErrorTypes());
        }
        return errors;
    }

    private String formatLessonObjectList(List<?> items) {
        List<String> lines = new ArrayList<>();
        int index = 1;
        for (Object item : items) {
            String text = formatLessonValue(item);
            if (!text.isBlank()) {
                lines.add(index++ + ". " + text);
            }
        }
        return String.join("\n", lines);
    }

    @SuppressWarnings("unchecked")
    private String formatLessonValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof List) {
            return ((List<?>) value).stream()
                    .map(this::formatLessonValue)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.joining("；"));
        }
        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            String mistake = firstNonBlank(map.get("mistake"), map.get("error"), map.get("problem"));
            String correction = firstNonBlank(map.get("correction"), map.get("fix"), map.get("solution"));
            if (!mistake.isBlank() || !correction.isBlank()) {
                List<String> parts = new ArrayList<>();
                if (!mistake.isBlank()) parts.add("误区：" + mistake);
                if (!correction.isBlank()) parts.add("纠正：" + correction);
                return String.join("；", parts);
            }
            return map.entrySet().stream()
                    .map(e -> e.getKey() + "：" + formatLessonValue(e.getValue()))
                    .filter(s -> !s.endsWith("："))
                    .collect(Collectors.joining("；"));
        }
        return String.valueOf(value);
    }

    private List<String> buildAssistantPrompts(String kpName, String strategyLabel) {
        List<String> prompts = new ArrayList<>();
        prompts.add("我刚开始学" + kpName + "，请按" + strategyLabel + "方式带我过一遍核心概念");
        prompts.add("请用一个小例题检查我是否理解了" + kpName);
        prompts.add("我做错了这道题，请只给提示，不要直接给完整答案");
        return prompts;
    }

    private List<Map<String, Object>> buildTracingPathEdges(List<Map<String, Object>> nodes) {
        List<Map<String, Object>> edges = new ArrayList<>();
        for (int i = 0; i < nodes.size() - 1; i++) {
            Map<String, Object> edge = new LinkedHashMap<>();
            edge.put("from", nodes.get(i).get("nodeId"));
            edge.put("to", nodes.get(i + 1).get("nodeId"));
            edge.put("relation", "prerequisite");
            edges.add(edge);
        }
        return edges;
    }

    private Map<String, Object> buildPathResponse(LearningPath path, List<Map<String, Object>> nodes) {
        Map<String, Object> result = new LinkedHashMap<>();
        String moduleName = resolvePathModule(path, nodes);
        result.put("pathId", path.getId());
        result.put("studentId", path.getStudentId());
        result.put("title", buildPathTitle(path));
        result.put("module", moduleName);
        result.put("tracingResultId", path.getTracingResultId());
        result.put("targetKnowledgePointId", path.getTargetKnowledgePointId());
        result.put("targetKnowledgePointName", path.getTargetKnowledgePointName());
        result.put("rootCausePointId", path.getRootCausePointId());
        result.put("rootCausePointName", path.getRootCausePointName());
        result.put("mode", path.getMode());
        result.put("status", path.getStatus());
        result.put("progress", toProgressPercent(path.getProgress()));
        result.put("totalEstimatedMinutes", path.getTotalEstimatedMinutes());
        result.put("actualStudyMinutes", path.getActualStudyMinutes());
        result.put("totalNodes", path.getTotalNodes());
        result.put("completedNodes", path.getCompletedNodes());
        result.put("nodes", nodes);
        result.put("profile", buildProfileInfo(path.getStudentId()));

        List<Map<String, Object>> tracingPath = parseJson(path.getTracingPath(),
                new TypeReference<List<Map<String, Object>>>() {}, new ArrayList<>());
        result.put("tracingPath", tracingPath);
        result.put("createdAt", path.getCreatedAt());
        result.put("lastStudyAt", path.getLastStudyAt());

        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> buildMasteryFromTracing(TracingResult tracingResult) {
        Map<String, Double> mastery = new HashMap<>();
        List<Map<String, Object>> rootCauses = parseJson(tracingResult.getMergedRootCauses(),
                new TypeReference<List<Map<String, Object>>>() {}, Collections.emptyList());
        for (Map<String, Object> rc : rootCauses) {
            String nodeId = String.valueOf(rc.getOrDefault("nodeId", ""));
            Object confObj = rc.get("confidence");
            if (!nodeId.isEmpty() && !"null".equals(nodeId)) {
                // 根因节点的掌握度一般较低，用 1 - confidence 估算
                double confidence = confObj instanceof Number ? ((Number) confObj).doubleValue() : 0.7;
                mastery.put(nodeId, Math.max(0.1, 1.0 - confidence));
            }
        }
        return mastery;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildWeakPointsFromTracing(TracingResult tracingResult) {
        List<Map<String, Object>> weakPoints = new ArrayList<>();
        List<Map<String, Object>> rootCauses = parseJson(tracingResult.getMergedRootCauses(),
                new TypeReference<List<Map<String, Object>>>() {}, Collections.emptyList());
        for (Map<String, Object> rc : rootCauses) {
            String nodeId = extractKnowledgePointId(rc);
            if (!nodeId.isBlank()) {
                Map<String, Object> wp = new LinkedHashMap<>();
                wp.put("kpId", nodeId);
                wp.put("kpName", resolveKpName(nodeId));
                wp.put("masteryLevel", 0.3);
                weakPoints.add(wp);
            }
        }
        return weakPoints;
    }

    /**
     * 查找练习题正确答案（从 path 节点数据中匹配）
     */
    private List<Map<String, Object>> buildCheckpointQuestionsFromSnapshots(LearningPath path,
                                                                            String nodeId,
                                                                            List<Map<String, Object>> answers) {
        if (answers == null || answers.isEmpty()) {
            throw new BusinessException(400, "检查点答案不能为空");
        }

        List<Map<String, Object>> checkpointQuestions = new ArrayList<>();
        for (int i = 0; i < answers.size(); i++) {
            Map<String, Object> ans = answers.get(i);
            String exerciseId = firstNonBlank(ans.get("exerciseId"), ans.get("questionId"));
            if (exerciseId.isBlank()) {
                throw new BusinessException(400, "检查点题目ID不能为空");
            }

            Map<String, Object> snapshot = findExerciseSnapshot(path, nodeId, exerciseId);
            List<Map<String, Object>> options = normalizeExerciseOptions(snapshot.get("options"));
            String correctAnswer = normalizeCorrectAnswer(firstNonBlank(snapshot.get("correctAnswer"), snapshot.get("answer")), options);
            if (correctAnswer.isBlank()) {
                throw new BusinessException(400, "检查点答案快照不存在，请重新生成课程后再提交");
            }

            String qId = firstNonBlank(ans.get("questionId"), ans.get("exerciseId"), "q_" + (i + 1));
            Map<String, Object> question = new LinkedHashMap<>();
            question.put("id", qId);
            question.put("exerciseId", exerciseId);
            question.put("question", firstNonBlank(
                    snapshot.get("question"), snapshot.get("content"), snapshot.get("problem")));
            question.put("correctAnswer", correctAnswer);
            question.put("difficulty", snapshot.getOrDefault("difficulty", 3));
            question.put("explanation", snapshot.getOrDefault("explanation", ""));
            if (!options.isEmpty()) {
                question.put("options", options);
            }
            checkpointQuestions.add(question);
        }
        return checkpointQuestions;
    }

    private Map<String, Object> buildRemediationPlan(String nodeId,
                                                     String kpId,
                                                     String kpName,
                                                     List<Map<String, Object>> questionResults) {
        List<Map<String, Object>> incorrect = questionResults.stream()
                .filter(r -> !Boolean.TRUE.equals(r.get("isCorrect")))
                .collect(Collectors.toList());
        List<String> focusErrors = incorrect.stream()
                .map(r -> firstNonBlank(r.get("errorType"), r.get("analysis")))
                .filter(s -> !s.isBlank())
                .limit(3)
                .collect(Collectors.toList());
        if (focusErrors.isEmpty()) {
            KnowledgeNode node = knowledgeGraphService.getNode(kpId);
            focusErrors = node != null ? collectCommonErrors(node).stream().limit(3).collect(Collectors.toList()) : Collections.emptyList();
        }

        List<String> actions = new ArrayList<>();
        actions.add("回看「" + kpName + "」核心概念和适用条件");
        actions.add("完成补救节点中的针对性练习");
        actions.add("订正错题后重新挑战检查点");

        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("nodeId", "remedial_" + nodeId);
        plan.put("remediationFor", nodeId);
        plan.put("knowledgePointId", kpId);
        plan.put("knowledgePointName", kpName);
        plan.put("focusErrors", focusErrors);
        plan.put("actions", actions);
        plan.put("estimatedMinutes", focusErrors.isEmpty() ? 15 : 20);
        return plan;
    }

    private String upsertRemediationNode(List<Map<String, Object>> nodes,
                                         String currentNodeId,
                                         Map<String, Object> remediationPlan) {
        String remediationNodeId = String.valueOf(remediationPlan.get("nodeId"));
        for (Map<String, Object> node : nodes) {
            if (remediationNodeId.equals(node.get("nodeId"))) {
                node.put("status", "unlocked");
                node.put("updatedAt", LocalDateTime.now().toString());
                return remediationNodeId;
            }
        }

        Map<String, Object> remediationNode = new LinkedHashMap<>();
        remediationNode.put("nodeId", remediationNodeId);
        remediationNode.put("order", nodes.size() + 1);
        remediationNode.put("type", "remediation");
        remediationNode.put("status", "unlocked");
        remediationNode.put("title", "补救：" + remediationPlan.get("knowledgePointName"));
        remediationNode.put("knowledgePointId", remediationPlan.get("knowledgePointId"));
        remediationNode.put("knowledgePointName", remediationPlan.get("knowledgePointName"));
        remediationNode.put("knowledgePoint", remediationPlan.get("knowledgePointName"));
        remediationNode.put("currentMastery", 0.0);
        remediationNode.put("teachingStrategy", "basic_consolidation");
        remediationNode.put("estimatedMinutes", remediationPlan.get("estimatedMinutes"));
        remediationNode.put("estimatedTime", remediationPlan.get("estimatedMinutes"));
        remediationNode.put("remediationFor", currentNodeId);
        remediationNode.put("focusErrors", remediationPlan.get("focusErrors"));
        remediationNode.put("reason", "检查点未通过后自动生成的补救节点");

        int insertAt = nodes.size();
        for (int i = 0; i < nodes.size(); i++) {
            if (currentNodeId.equals(nodes.get(i).get("nodeId"))) {
                insertAt = i + 1;
                break;
            }
        }
        nodes.add(insertAt, remediationNode);
        for (int i = 0; i < nodes.size(); i++) {
            nodes.get(i).put("order", i + 1);
        }
        return remediationNodeId;
    }

    private Map<String, String> buildCheckpointAnswers(List<Map<String, Object>> checkpointQuestions,
                                                       List<Map<String, Object>> answers) {
        Map<String, String> checkpointAnswers = new LinkedHashMap<>();
        for (int i = 0; i < checkpointQuestions.size(); i++) {
            Map<String, Object> question = checkpointQuestions.get(i);
            Map<String, Object> answer = answers.get(i);
            checkpointAnswers.put(
                    String.valueOf(question.get("id")),
                    firstNonBlank(answer.get("answer"), answer.get("studentAnswer")));
        }
        return checkpointAnswers;
    }

    private Map<String, Boolean> evaluateCheckpointLocally(List<Map<String, Object>> checkpointQuestions,
                                                           Map<String, String> checkpointAnswers) {
        Map<String, Boolean> correctness = new LinkedHashMap<>();
        for (Map<String, Object> question : checkpointQuestions) {
            String qId = String.valueOf(question.get("id"));
            String studentAnswer = checkpointAnswers.getOrDefault(qId, "");
            String correctAnswer = firstNonBlank(question.get("correctAnswer"));
            correctness.put(qId, !studentAnswer.isBlank() && studentAnswer.equalsIgnoreCase(correctAnswer));
        }
        return correctness;
    }

    private List<Map<String, Object>> buildLocalCheckpointResults(List<Map<String, Object>> checkpointQuestions,
                                                                  Map<String, String> checkpointAnswers,
                                                                  Map<String, Boolean> correctness) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> question : checkpointQuestions) {
            String qId = String.valueOf(question.get("id"));
            boolean correct = Boolean.TRUE.equals(correctness.get(qId));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("questionId", qId);
            result.put("exerciseId", question.get("exerciseId"));
            result.put("isCorrect", correct);
            result.put("yourAnswer", checkpointAnswers.getOrDefault(qId, ""));
            result.put("correctAnswer", question.get("correctAnswer"));
            result.put("analysis", correct
                    ? "回答正确"
                    : firstNonBlank(question.get("explanation"), "请回顾本题对应知识点后再订正。"));
            results.add(result);
        }
        return results;
    }

    private String firstNonBlank(Object... values) {
        for (Object value : values) {
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value).trim();
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private String findCorrectAnswer(LearningPath path, String nodeId, String exerciseId) {
        Map<String, Object> exercise = findExerciseSnapshot(path, nodeId, exerciseId);
        String correctAnswer = normalizeCorrectAnswer(
                firstNonBlank(exercise.get("correctAnswer"), exercise.get("answer")),
                normalizeExerciseOptions(exercise.get("options")));
        if (correctAnswer.isBlank()) {
            throw new BusinessException(400, "练习题答案快照不存在，请重新生成课程后再提交");
        }
        return correctAnswer;
    }

    private String findExerciseExplanation(LearningPath path, String nodeId, String exerciseId) {
        Map<String, Object> exercise = findExerciseSnapshot(path, nodeId, exerciseId);
        Object explanation = exercise.get("explanation");
        return explanation != null ? explanation.toString() : "";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findExerciseSnapshot(LearningPath path, String nodeId, String exerciseId) {
        Map<String, Object> snapshots = parseJson(path.getLessonSnapshots(),
                new TypeReference<Map<String, Object>>() {}, Collections.emptyMap());
        Object lessonObj = snapshots.get(nodeId);
        if (!(lessonObj instanceof Map)) {
            throw new BusinessException(400, "课程快照不存在，请先打开课程生成练习题");
        }

        Map<String, Object> lessonSnapshot = (Map<String, Object>) lessonObj;
        Object lessonDataObj = lessonSnapshot.get("lesson");
        if (!(lessonDataObj instanceof Map)) {
            throw new BusinessException(400, "课程快照格式不完整，请重新生成课程");
        }

        Map<String, Object> lessonData = (Map<String, Object>) lessonDataObj;
        Object exercisesObj = lessonData.get("exercises");
        if (!(exercisesObj instanceof List)) {
            throw new BusinessException(400, "课程快照中没有练习题");
        }

        for (Object item : (List<?>) exercisesObj) {
            if (!(item instanceof Map)) continue;
            Map<String, Object> exercise = (Map<String, Object>) item;
            String candidateId = String.valueOf(exercise.getOrDefault("exerciseId",
                    exercise.getOrDefault("id", "")));
            if (Objects.equals(candidateId, exerciseId)) {
                return exercise;
            }
        }

        throw new BusinessException(404, "练习题不存在或已过期，请重新生成课程");
    }

    @SuppressWarnings("unchecked")
    private void saveLessonSnapshot(LearningPath path, String nodeId, Map<String, Object> lessonResponse) {
        Map<String, Object> snapshots = new LinkedHashMap<>(parseJson(path.getLessonSnapshots(),
                new TypeReference<Map<String, Object>>() {}, Collections.emptyMap()));
        lessonResponse.put(SNAPSHOT_VERSION_KEY, LESSON_SNAPSHOT_VERSION);
        snapshots.put(nodeId, lessonResponse);
        try {
            path.setLessonSnapshots(objectMapper.writeValueAsString(snapshots));
            path.setLastStudyAt(LocalDateTime.now());
            learningPathRepository.save(path);
        } catch (Exception e) {
            throw new BusinessException(500, "保存课程快照失败");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findVideoSnapshot(LearningPath path, String nodeId) {
        Map<String, Object> snapshots = parseJson(path.getLessonSnapshots(),
                new TypeReference<Map<String, Object>>() {}, Collections.emptyMap());
        Object lessonObj = snapshots.get(nodeId);
        if (!(lessonObj instanceof Map)) {
            return Collections.emptyMap();
        }
        Object videoObj = ((Map<String, Object>) lessonObj).get("videoResource");
        if (videoObj instanceof Map) {
            return new LinkedHashMap<>((Map<String, Object>) videoObj);
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findLessonSnapshotResponse(LearningPath path, String nodeId) {
        Map<String, Object> snapshots = parseJson(path.getLessonSnapshots(),
                new TypeReference<Map<String, Object>>() {}, Collections.emptyMap());
        Object lessonObj = snapshots.get(nodeId);
        if (!(lessonObj instanceof Map)) {
            return Collections.emptyMap();
        }

        Map<String, Object> lessonSnapshot = new LinkedHashMap<>((Map<String, Object>) lessonObj);
        // 版本不匹配的旧快照（结构已变更）视为未命中，自动重新生成，避免手动清库
        Object versionObj = lessonSnapshot.get(SNAPSHOT_VERSION_KEY);
        int version = versionObj instanceof Number ? ((Number) versionObj).intValue() : 0;
        if (version != LESSON_SNAPSHOT_VERSION) {
            log.warn("课程快照版本不匹配（快照v{} 期望v{}），视为未命中并重新生成: nodeId={}",
                    version, LESSON_SNAPSHOT_VERSION, nodeId);
            return Collections.emptyMap();
        }

        Object lessonData = lessonSnapshot.get("lesson");
        Object contentData = lessonSnapshot.get("content");
        if (!(lessonData instanceof Map) && !(contentData instanceof Map)) {
            return Collections.emptyMap();
        }
        // 命中前校验讲解是否有实质内容：历史上讲解生成失败时会存下 sections 为空的坏快照，
        // 这类快照若直接命中会永久绕过重新生成，导致“重开依旧讲解失败”。视为未命中以触发重生。
        if (!snapshotHasLessonContent(lessonSnapshot)) {
            log.warn("命中的课程快照讲解内容为空，视为未命中并重新生成: nodeId={}", nodeId);
            return Collections.emptyMap();
        }
        return lessonSnapshot;
    }

    /** 判断课程快照是否包含有实质内容的讲解 section（content 非空）。 */
    @SuppressWarnings("unchecked")
    private boolean snapshotHasLessonContent(Map<String, Object> lessonSnapshot) {
        Object contentObj = lessonSnapshot.get("content");
        if (contentObj instanceof Map) {
            Object sectionsObj = ((Map<String, Object>) contentObj).get("sections");
            if (sectionsObj instanceof List) {
                for (Object section : (List<?>) sectionsObj) {
                    if (section instanceof Map) {
                        Object body = ((Map<String, Object>) section).get("body");
                        Object content = ((Map<String, Object>) section).get("content");
                        if (!String.valueOf(body == null ? content : body).isBlank()
                                && !"null".equals(String.valueOf(body == null ? content : body))) {
                            return true;
                        }
                    }
                }
            }
        }
        Object lessonObj = lessonSnapshot.get("lesson");
        if (lessonObj instanceof Map) {
            Object sectionsObj = ((Map<String, Object>) lessonObj).get("sections");
            if (sectionsObj instanceof List) {
                for (Object section : (List<?>) sectionsObj) {
                    if (section instanceof Map) {
                        Object content = ((Map<String, Object>) section).get("content");
                        if (content != null && !String.valueOf(content).isBlank()
                                && !"null".equals(String.valueOf(content))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void saveVideoSnapshot(LearningPath path, String nodeId, Map<String, Object> videoResource) {
        Map<String, Object> snapshots = new LinkedHashMap<>(parseJson(path.getLessonSnapshots(),
                new TypeReference<Map<String, Object>>() {}, Collections.emptyMap()));
        Map<String, Object> lessonSnapshot = new LinkedHashMap<>();
        Object existing = snapshots.get(nodeId);
        if (existing instanceof Map) {
            lessonSnapshot.putAll((Map<String, Object>) existing);
        }
        lessonSnapshot.put("videoResource", videoResource);
        snapshots.put(nodeId, lessonSnapshot);
        try {
            path.setLessonSnapshots(objectMapper.writeValueAsString(snapshots));
            path.setLastStudyAt(LocalDateTime.now());
            learningPathRepository.save(path);
        } catch (Exception e) {
            throw new BusinessException(500, "保存视频快照失败");
        }
    }

    @SuppressWarnings("unchecked")
    private void enrichLessonResponseForFrontend(Map<String, Object> result,
                                                 Map<String, Object> lesson,
                                                 String kpName) {
        result.put("title", lesson.getOrDefault("title", kpName + "课程"));
        result.put("type", "exercise");
        result.put("isCheckpoint", false);
        result.put("knowledgePoint", kpName);

        Map<String, Object> content = new LinkedHashMap<>();
        List<Map<String, Object>> frontendSections = new ArrayList<>();
        List<Object> keyFormulas = new ArrayList<>();
        List<Map<String, Object>> examples = new ArrayList<>();

        Object sectionsObj = lesson.get("sections");
        if (sectionsObj instanceof List) {
            for (Object sectionObj : (List<?>) sectionsObj) {
                if (!(sectionObj instanceof Map)) continue;
                Map<String, Object> section = (Map<String, Object>) sectionObj;
                String type = String.valueOf(section.getOrDefault("type", ""));
                if ("concept".equals(type)) {
                    Map<String, Object> frontendSection = new LinkedHashMap<>();
                    frontendSection.put("title", section.getOrDefault("title", "核心概念"));
                    frontendSection.put("body", section.getOrDefault("content", ""));
                    frontendSections.add(frontendSection);
                    Object formulasObj = section.get("keyFormulas");
                    if (formulasObj instanceof List) {
                        keyFormulas.addAll((List<?>) formulasObj);
                    }
                } else if ("example".equals(type)) {
                    Object contentObj = section.get("content");
                    if (contentObj instanceof List) {
                        for (Object exampleObj : (List<?>) contentObj) {
                            if (!(exampleObj instanceof Map)) continue;
                            Map<String, Object> raw = (Map<String, Object>) exampleObj;
                            Map<String, Object> example = new LinkedHashMap<>();
                            example.put("question", raw.getOrDefault("problem", raw.getOrDefault("title", "")));
                            example.put("steps", splitSolutionSteps(raw.get("solution")));
                            example.put("answer", raw.getOrDefault("keyPoint", ""));
                            examples.add(example);
                        }
                    }
                } else {
                    Map<String, Object> frontendSection = new LinkedHashMap<>();
                    frontendSection.put("title", section.getOrDefault("title", "学习要点"));
                    frontendSection.put("body", section.getOrDefault("content", ""));
                    frontendSections.add(frontendSection);
                }
            }
        }

        content.put("sections", frontendSections);
        content.put("keyFormulas", keyFormulas);
        content.put("examples", examples);
        result.put("content", content);

        Object exercisesObj = lesson.get("exercises");
        List<Map<String, Object>> frontendExercises = new ArrayList<>();
        if (exercisesObj instanceof List) {
            for (Object exerciseObj : (List<?>) exercisesObj) {
                if (!(exerciseObj instanceof Map)) continue;
                Map<String, Object> exercise = (Map<String, Object>) exerciseObj;
                frontendExercises.add(toFrontendExercise(exercise));
            }
        }
        result.put("exercises", frontendExercises);
    }

    private List<String> splitSolutionSteps(Object solutionObj) {
        if (solutionObj == null) return Collections.emptyList();
        String solution = solutionObj.toString();
        if (solution.isBlank()) return Collections.emptyList();
        return Arrays.stream(solution.split("\\n+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toFrontendExercise(Map<String, Object> exercise) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("exerciseId", exercise.getOrDefault("exerciseId", exercise.getOrDefault("id", "")));
        item.put("content", firstNonBlank(
                exercise.get("content"), exercise.get("problem"), exercise.get("question"), exercise.get("title")));
        item.put("difficulty", exercise.getOrDefault("difficulty", 2));
        item.put("options", normalizeExerciseOptions(exercise.get("options")));
        return item;
    }

    private Map<String, Object> normalizeLessonExercise(Map<String, Object> exercise, int index, String nodeId) {
        String exerciseId = firstNonBlank(exercise.get("exerciseId"), exercise.get("id"));
        if (exerciseId.isBlank()) {
            exerciseId = "ex_" + nodeId + "_" + (index + 1);
        }
        exercise.put("exerciseId", exerciseId);

        List<Map<String, Object>> options = normalizeExerciseOptions(exercise.get("options"));
        exercise.put("options", options);

        String correctAnswer = normalizeCorrectAnswer(
                firstNonBlank(exercise.get("correctAnswer"), exercise.get("answer")),
                options);
        if (!correctAnswer.isBlank()) {
            exercise.put("correctAnswer", correctAnswer);
            exercise.put("answer", correctAnswer);
        }
        return exercise;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeExerciseOptions(Object optionsObj) {
        List<Map<String, Object>> options = new ArrayList<>();
        if (optionsObj instanceof Map) {
            Map<String, Object> rawOptions = (Map<String, Object>) optionsObj;
            for (Map.Entry<String, Object> entry : rawOptions.entrySet()) {
                options.add(normalizeOptionEntry(entry.getKey(), entry.getValue(), options.size()));
            }
        } else if (optionsObj instanceof List) {
            for (Object optionObj : (List<?>) optionsObj) {
                options.add(normalizeOption(optionObj, options.size()));
            }
        } else if (optionsObj != null) {
            String raw = String.valueOf(optionsObj);
            for (String part : raw.split("\\n+")) {
                if (!part.isBlank()) {
                    options.add(normalizeOption(part, options.size()));
                }
            }
        }
        return options.stream()
                .filter(option -> !firstNonBlank(option.get("key")).isBlank())
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeOptionEntry(String rawKey, Object rawValue, int index) {
        if (rawValue instanceof Map) {
            Map<String, Object> option = normalizeOption(rawValue, index);
            String key = extractOptionKey(rawKey);
            if (key.isBlank()) {
                key = fallbackOptionKey(index);
            }
            option.put("key", key);
            option.put("label", key);
            return option;
        }

        String key = extractOptionKey(rawKey);
        if (key.isBlank()) {
            key = fallbackOptionKey(index);
        }
        String text = stripOptionPrefix(firstNonBlank(rawValue));
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("key", key);
        option.put("label", key);
        option.put("text", text);
        return option;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeOption(Object optionObj, int index) {
        if (optionObj instanceof Map) {
            Map<String, Object> raw = (Map<String, Object>) optionObj;
            String rawKey = firstNonBlank(
                    raw.get("key"), raw.get("label"), raw.get("optionKey"), raw.get("option"), raw.get("id"));
            String rawText = firstNonBlank(
                    raw.get("text"), raw.get("content"), raw.get("description"), raw.get("value"), raw.get("name"));
            if (rawText.isBlank() && !rawKey.isBlank() && extractOptionKey(rawKey).isBlank()) {
                rawText = rawKey;
            }

            String key = extractOptionKey(firstNonBlank(rawKey, rawText));
            if (key.isBlank()) {
                key = fallbackOptionKey(index);
            }

            Map<String, Object> option = new LinkedHashMap<>(raw);
            option.put("key", key);
            option.put("label", key);
            option.put("text", stripOptionPrefix(rawText));
            return option;
        }

        String text = firstNonBlank(optionObj);
        String key = extractOptionKey(text);
        if (key.isBlank()) {
            key = fallbackOptionKey(index);
        }

        Map<String, Object> option = new LinkedHashMap<>();
        option.put("key", key);
        option.put("label", key);
        option.put("text", stripOptionPrefix(text));
        return option;
    }

    private String normalizeCorrectAnswer(String answer, List<Map<String, Object>> options) {
        if (answer == null || answer.isBlank()) {
            return "";
        }

        String extracted = extractOptionKey(answer);
        if (!extracted.isBlank() && optionKeyExists(extracted, options)) {
            return extracted;
        }

        for (Map<String, Object> option : options) {
            String key = firstNonBlank(option.get("key"), option.get("label"));
            if (!key.isBlank() && answer.equalsIgnoreCase(key)) {
                return key;
            }
        }

        String strippedAnswer = stripOptionPrefix(answer);
        for (Map<String, Object> option : options) {
            String key = firstNonBlank(option.get("key"), option.get("label"));
            String text = firstNonBlank(option.get("text"), option.get("content"), option.get("value"));
            if (!key.isBlank() && !text.isBlank()
                    && (answer.equalsIgnoreCase(text) || strippedAnswer.equalsIgnoreCase(text))) {
                return key;
            }
        }

        return !extracted.isBlank() ? extracted : answer.trim();
    }

    private boolean optionKeyExists(String key, List<Map<String, Object>> options) {
        for (Map<String, Object> option : options) {
            if (key.equalsIgnoreCase(firstNonBlank(option.get("key"), option.get("label")))) {
                return true;
            }
        }
        return false;
    }

    private String extractOptionKey(Object value) {
        String text = firstNonBlank(value);
        if (text.isBlank()) {
            return "";
        }
        String normalized = text.trim();
        if (normalized.matches("(?i)^[a-z]$") || normalized.matches("(?i)^[a-z][.、):：\\s].*")) {
            return normalized.substring(0, 1).toUpperCase(Locale.ROOT);
        }
        return "";
    }

    private String stripOptionPrefix(Object value) {
        return firstNonBlank(value).replaceFirst("(?i)^[a-z][.、):：\\s]+", "").trim();
    }

    private String fallbackOptionKey(int index) {
        if (index >= 0 && index < OPTION_KEY_FALLBACKS.length) {
            return OPTION_KEY_FALLBACKS[index];
        }
        return "O" + (index + 1);
    }

    private void updateNodeMastery(LearningPath path, List<Map<String, Object>> nodes, String nodeId, double newMastery) {
        for (Map<String, Object> node : nodes) {
            if (nodeId.equals(node.get("nodeId"))) {
                node.put("currentMastery", newMastery);
                break;
            }
        }
        try {
            path.setNodes(objectMapper.writeValueAsString(nodes));
        } catch (Exception ignored) {}
    }

    private void unlockNextLockedNode(List<Map<String, Object>> nodes, String currentNodeId) {
        boolean foundCurrent = false;
        for (Map<String, Object> node : nodes) {
            if (foundCurrent && "locked".equals(String.valueOf(node.getOrDefault("status", "")))) {
                node.put("status", "unlocked");
                return;
            }
            if (currentNodeId.equals(node.get("nodeId"))) {
                foundCurrent = true;
            }
        }
    }

    private void markNodeFailed(List<Map<String, Object>> nodes, String nodeId, double mastery) {
        for (Map<String, Object> node : nodes) {
            if (nodeId.equals(node.get("nodeId"))) {
                node.put("status", "failed");
                node.put("currentMastery", mastery);
                return;
            }
        }
    }

    private void completeRemediatedSourceNode(List<Map<String, Object>> nodes,
                                              Map<String, Object> completedNode,
                                              double mastery) {
        Object remediationFor = completedNode.get("remediationFor");
        if (remediationFor == null) {
            return;
        }
        String sourceNodeId = String.valueOf(remediationFor);
        for (Map<String, Object> node : nodes) {
            if (sourceNodeId.equals(node.get("nodeId"))) {
                node.put("status", "completed");
                node.put("currentMastery", mastery);
                return;
            }
        }
    }

    private BigDecimal toRatioProgress(int completed, int total) {
        if (total <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf((double) completed / total)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal toProgressPercent(BigDecimal ratio) {
        if (ratio == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal normalized = ratio.compareTo(BigDecimal.ONE) <= 0
                ? ratio.multiply(BigDecimal.valueOf(100))
                : ratio;
        return normalized.setScale(0, RoundingMode.HALF_UP);
    }

    private String findNextActionableNodeId(List<Map<String, Object>> nodes) {
        for (Map<String, Object> node : nodes) {
            String status = String.valueOf(node.getOrDefault("status", ""));
            if ("unlocked".equals(status) || "in_progress".equals(status) || "pending".equals(status)) {
                return String.valueOf(node.get("nodeId"));
            }
        }
        return null;
    }

    private String resolveKpNameFromNodes(List<Map<String, Object>> nodes, String nodeId) {
        return nodes.stream()
                .filter(n -> nodeId.equals(n.get("nodeId")))
                .map(n -> (String) n.getOrDefault("knowledgePointName", ""))
                .findFirst()
                .orElse("下一知识点");
    }

    private String resolveKpName(String kpId) {
        if (kpId == null) return "未知知识点";
        KnowledgeNode node = knowledgeGraphService.getNode(kpId);
        return node != null ? node.getName() : kpId;
    }

    @SuppressWarnings("unchecked")
    private String resolveKpName(String kpId, Map<String, Object> pointIndex) {
        if (kpId == null) return "未知知识点";
        KnowledgeNode node = knowledgeGraphService.getNode(kpId);
        if (node != null) {
            return node.getName();
        }
        Object indexed = pointIndex != null ? pointIndex.get(kpId) : null;
        if (indexed instanceof Map) {
            Map<String, Object> data = (Map<String, Object>) indexed;
            String name = firstNonBlank(data.get("knowledgePointName"), data.get("nodeName"),
                    data.get("targetPointName"), data.get("kpName"));
            if (!name.isBlank()) {
                return name;
            }
        }
        return kpId;
    }

    private String buildPathTitle(LearningPath path) {
        String root = path.getRootCausePointName();
        String target = path.getTargetKnowledgePointName();
        if (root != null && !root.isBlank() && target != null && !target.isBlank() && !root.equals(target)) {
            return root + " -> " + target + " 学习闭环";
        }
        if (target != null && !target.isBlank()) {
            return target + " 学习闭环";
        }
        return "个性化学习闭环";
    }

    private String getModuleForId(String kpId) {
        if (kpId == null) return "高校课程";
        KnowledgeNode node = knowledgeGraphService.getNode(kpId);
        return node != null ? node.getModule() : "高校课程";
    }

    private String resolveNodeModule(Map<String, Object> pathNode, KnowledgeNode kpNode) {
        return firstNonBlank(
                pathNode != null ? pathNode.get("module") : null,
                kpNode != null ? kpNode.getModule() : null,
                "高校课程");
    }

    private String resolvePathModule(LearningPath path, List<Map<String, Object>> nodes) {
        String graphModule = getModuleForId(path.getTargetKnowledgePointId());
        if (!"高校课程".equals(graphModule)) {
            return graphModule;
        }
        for (Map<String, Object> node : nodes) {
            String module = firstNonBlank(node.get("module"));
            if (!module.isBlank() && !"高校课程".equals(module)) {
                return module;
            }
        }
        return "高校课程";
    }

    private Map<String, Object> findPathNode(LearningPath path, String nodeId) {
        List<Map<String, Object>> nodes = parseJson(path.getNodes(),
                new TypeReference<List<Map<String, Object>>>() {}, Collections.emptyList());
        for (Map<String, Object> node : nodes) {
            if (nodeId.equals(String.valueOf(node.get("nodeId")))) {
                return node;
            }
        }
        return Collections.emptyMap();
    }

    private String determineStrategy(String priority) {
        switch (priority) {
            case "high": return "basic_consolidation";
            case "low": return "expansion";
            default: return "strengthening";
        }
    }

    private <T> T parseJson(String json, TypeReference<T> typeRef, T defaultValue) {
        if (json == null || json.isEmpty()) return defaultValue;
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            try {
                String unwrapped = objectMapper.readValue(json, String.class);
                if (unwrapped != null && !unwrapped.isBlank() && !unwrapped.equals(json)) {
                    return objectMapper.readValue(unwrapped, typeRef);
                }
            } catch (Exception ignored) {
            }
            return defaultValue;
        }
    }
}
