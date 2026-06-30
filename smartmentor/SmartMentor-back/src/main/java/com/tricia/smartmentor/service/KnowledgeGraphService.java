package com.tricia.smartmentor.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KnowledgeGraphService {

    private final ResourcePatternResolver resourcePatternResolver;
    private final ObjectMapper objectMapper;

    private final Map<String, KnowledgeNode> nodeMap = new HashMap<>();

    public KnowledgeGraphService(ResourcePatternResolver resourcePatternResolver, ObjectMapper objectMapper) {
        this.resourcePatternResolver = resourcePatternResolver;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        String[] files = {
                "classpath:knowledge-graph/computer-ai-foundation.json",
                "classpath:knowledge-graph/software-java-web.json",
                "classpath:knowledge-graph/electronic-digital-circuit.json"
        };

        for (String file : files) {
            try {
                Resource resource = resourcePatternResolver.getResource(file);
                if (!resource.exists()) {
                    log.warn("Knowledge graph file not found: {}", file);
                    continue;
                }
                try (InputStream is = resource.getInputStream()) {
                    List<KnowledgeNode> nodes = objectMapper.readValue(is, new TypeReference<List<KnowledgeNode>>() {});
                    for (KnowledgeNode node : nodes) {
                        nodeMap.put(node.getId(), node);
                    }
                    log.info("Loaded {} nodes from {}", nodes.size(), file);
                }
            } catch (Exception e) {
                log.error("Failed to load knowledge graph file: {}", file, e);
            }
        }

        log.info("Knowledge graph initialized with {} total nodes", nodeMap.size());
    }

    /**
     * 获取单个知识点
     */
    public KnowledgeNode getNode(String id) {
        KnowledgeNode node = nodeMap.get(id);
        if (node == null) {
            return null;
        }
        return copyNode(node);
    }

    /**
     * 获取某模块所有知识点
     */
    public List<KnowledgeNode> getNodesByModule(String module) {
        String normalizedModule = normalizeModuleName(module);
        return nodeMap.values().stream()
                .filter(node -> normalizedModule.equals(normalizeModuleName(node.getModule())))
                .map(this::copyNode)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Normalize frontend-facing Chinese module labels and backend module keys.
     */
    public String normalizeModuleName(String module) {
        if (module == null) {
            return "";
        }
        switch (module.trim()) {
            case "人工智能基础":
            case "ai-foundation":
            case "computer-ai-foundation":
                return "人工智能基础";
            case "Java Web 开发":
            case "Java Web":
            case "java-web":
            case "software-java-web":
                return "Java Web 开发";
            case "数字电路基础":
            case "digital-circuit":
            case "electronic-digital-circuit":
                return "数字电路基础";
            default:
                return module.trim();
        }
    }

    /**
     * 获取直接前置依赖
     */
    public List<KnowledgeNode> getPrerequisites(String id) {
        KnowledgeNode node = nodeMap.get(id);
        if (node == null || node.getPrerequisites() == null) {
            return Collections.emptyList();
        }
        return node.getPrerequisites().stream()
                .map(nodeMap::get)
                .filter(Objects::nonNull)
                .map(this::copyNode)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * 递归获取所有前置依赖（BFS）
     */
    public List<KnowledgeNode> getAllPrerequisitesRecursive(String id) {
        KnowledgeNode startNode = nodeMap.get(id);
        if (startNode == null) {
            return Collections.emptyList();
        }

        Set<String> visited = new LinkedHashSet<>();
        Queue<String> queue = new LinkedList<>();

        if (startNode.getPrerequisites() != null) {
            for (String prereqId : startNode.getPrerequisites()) {
                if (!visited.contains(prereqId)) {
                    visited.add(prereqId);
                    queue.add(prereqId);
                }
            }
        }

        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            KnowledgeNode current = nodeMap.get(currentId);
            if (current != null && current.getPrerequisites() != null) {
                for (String prereqId : current.getPrerequisites()) {
                    if (!visited.contains(prereqId)) {
                        visited.add(prereqId);
                        queue.add(prereqId);
                    }
                }
            }
        }

        return visited.stream()
                .map(nodeMap::get)
                .filter(Objects::nonNull)
                .map(this::copyNode)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * 获取依赖此节点的所有节点
     */
    public List<KnowledgeNode> getDependents(String id) {
        return nodeMap.values().stream()
                .filter(node -> node.getPrerequisites() != null && node.getPrerequisites().contains(id))
                .map(this::copyNode)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * 按难度范围筛选
     */
    public List<KnowledgeNode> getNodesByDifficulty(String module, int minDiff, int maxDiff) {
        String normalizedModule = normalizeModuleName(module);
        return nodeMap.values().stream()
                .filter(node -> normalizedModule.equals(normalizeModuleName(node.getModule())))
                .filter(node -> node.getDifficulty() >= minDiff && node.getDifficulty() <= maxDiff)
                .map(this::copyNode)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * 对给定节点列表做拓扑排序（Kahn算法，用于学习路径）
     */
    public List<KnowledgeNode> topologicalSort(List<String> nodeIds) {
        Set<String> nodeIdSet = new LinkedHashSet<>(nodeIds);

        // 构建子图的入度表和邻接表
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        Map<String, List<String>> adjacency = new LinkedHashMap<>();

        for (String nid : nodeIdSet) {
            inDegree.put(nid, 0);
            adjacency.put(nid, new ArrayList<>());
        }

        // 计算入度：在子图范围内，如果A是B的前置，则A->B的边，B的入度+1
        for (String nid : nodeIdSet) {
            KnowledgeNode node = nodeMap.get(nid);
            if (node != null && node.getPrerequisites() != null) {
                for (String prereqId : node.getPrerequisites()) {
                    if (nodeIdSet.contains(prereqId)) {
                        adjacency.get(prereqId).add(nid);
                        inDegree.put(nid, inDegree.get(nid) + 1);
                    }
                }
            }
        }

        // Kahn算法
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<String> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            String current = queue.poll();
            sorted.add(current);
            for (String neighbor : adjacency.get(current)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    queue.add(neighbor);
                }
            }
        }

        // 如果有环，将未排序的节点追加到末尾
        if (sorted.size() < nodeIdSet.size()) {
            log.warn("Cycle detected in knowledge graph subset, appending remaining nodes");
            for (String nid : nodeIdSet) {
                if (!sorted.contains(nid)) {
                    sorted.add(nid);
                }
            }
        }

        return sorted.stream()
                .map(nodeMap::get)
                .filter(Objects::nonNull)
                .map(this::copyNode)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * 找到弱点节点的共同前置根因节点。
     * 对每个weakNode做BFS找前置，统计出现频率最高的非叶子节点作为根因。
     */
    public List<KnowledgeNode> findRootCauses(List<String> weakNodeIds) {
        if (weakNodeIds == null || weakNodeIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 统计每个前置节点出现的频率
        Map<String, Integer> frequencyMap = new HashMap<>();

        for (String weakId : weakNodeIds) {
            KnowledgeNode weakNode = nodeMap.get(weakId);
            if (weakNode == null) {
                continue;
            }

            // BFS找所有前置
            Set<String> visited = new HashSet<>();
            Queue<String> queue = new LinkedList<>();

            if (weakNode.getPrerequisites() != null) {
                for (String prereqId : weakNode.getPrerequisites()) {
                    if (!visited.contains(prereqId)) {
                        visited.add(prereqId);
                        queue.add(prereqId);
                    }
                }
            }

            while (!queue.isEmpty()) {
                String currentId = queue.poll();
                KnowledgeNode current = nodeMap.get(currentId);
                if (current != null && current.getPrerequisites() != null) {
                    for (String prereqId : current.getPrerequisites()) {
                        if (!visited.contains(prereqId)) {
                            visited.add(prereqId);
                            queue.add(prereqId);
                        }
                    }
                }
            }

            // 统计非叶子节点的频率
            for (String visitedId : visited) {
                if (!isLeafNode(visitedId)) {
                    frequencyMap.merge(visitedId, 1, Integer::sum);
                }
            }
        }

        if (frequencyMap.isEmpty()) {
            return Collections.emptyList();
        }

        // 找到最高频率
        int maxFrequency = frequencyMap.values().stream().mapToInt(Integer::intValue).max().orElse(0);

        // 频率最高的候选集
        Set<String> topCandidates = frequencyMap.entrySet().stream()
                .filter(entry -> entry.getValue() == maxFrequency)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        // M10：在候选中只保留"最底层"根因——其自身前置不再属于候选集的节点。
        // 否则单薄弱点时整条前置链频率都为 1 并列，会把中间节点也当根因；
        // 过滤后只剩链条最底部的源节点，符合"自底向上回溯到最底层根因"的定义。
        List<String> deepest = topCandidates.stream()
                .filter(id -> {
                    KnowledgeNode n = nodeMap.get(id);
                    if (n == null || n.getPrerequisites() == null || n.getPrerequisites().isEmpty()) {
                        return true; // 无前置 = 源节点
                    }
                    return n.getPrerequisites().stream().noneMatch(topCandidates::contains);
                })
                .collect(Collectors.toList());

        List<String> finalRoots = deepest.isEmpty() ? new ArrayList<>(topCandidates) : deepest;

        return finalRoots.stream()
                .map(nodeMap::get)
                .filter(Objects::nonNull)
                .map(this::copyNode)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * 课程 -> 专业方向 映射（module 即课程名）
     */
    private static final Map<String, String> COURSE_MAJOR = Map.of(
            "人工智能基础", "计算机类",
            "Java Web 开发", "软件工程",
            "数字电路基础", "电子信息"
    );

    /**
     * 按专业方向、当前课程、学历层次过滤知识点。
     * 优先用课程过滤；课程缺省或无匹配时回退到专业方向。
     * 若都无匹配（图谱外的任意科目），返回空列表——由上层交给 LLM 凭科目名自由出题，
     * 不再误返回全部知识点（否则会用其它科目的题诊断当前科目）。
     */
    public List<KnowledgeNode> findNodesByCourse(String majorDirection, String currentCourse, String educationLevel) {
        List<KnowledgeNode> byCourse = Collections.emptyList();
        if (currentCourse != null && !currentCourse.isBlank()) {
            byCourse = getNodesByModule(currentCourse);
        }
        if (byCourse.isEmpty() && majorDirection != null && !majorDirection.isBlank()) {
            byCourse = nodeMap.values().stream()
                    .filter(node -> majorDirection.equals(resolveMajor(node)))
                    .map(this::copyNode)
                    .collect(Collectors.toList());
        }
        if (byCourse.isEmpty()) {
            // 图谱外科目：返回空，让诊断走「任意科目」分支（LLM 凭科目名出题）
            return Collections.emptyList();
        }
        if (educationLevel == null || educationLevel.isBlank()) {
            return Collections.unmodifiableList(byCourse);
        }
        return byCourse.stream()
                .filter(node -> node.getEducationLevels() == null
                        || node.getEducationLevels().isEmpty()
                        || node.getEducationLevels().contains(educationLevel))
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * 判断某科目/课程是否为知识图谱中已预定义的科目（用于诊断区分预定义 vs 任意科目）。
     */
    public boolean isKnownModule(String module) {
        if (module == null || module.isBlank()) {
            return false;
        }
        return !getNodesByModule(module).isEmpty();
    }

    /**
     * 获取节点的直接前置（按方案命名，等价于 getPrerequisites）
     */
    public List<KnowledgeNode> findPrerequisites(String nodeId) {
        return getPrerequisites(nodeId);
    }

    /**
     * 列出某专业方向下可学习的课程列表
     */
    public List<String> listCoursesByMajor(String majorDirection) {
        Set<String> courses = new LinkedHashSet<>();
        for (KnowledgeNode node : nodeMap.values()) {
            String course = resolveCourse(node);
            if (course.isBlank()) {
                continue;
            }
            if (majorDirection == null || majorDirection.isBlank()
                    || majorDirection.equals(resolveMajor(node))) {
                courses.add(course);
            }
        }
        if (courses.isEmpty()) {
            // 专业方向无直接匹配时，返回全部课程
            for (KnowledgeNode node : nodeMap.values()) {
                String course = resolveCourse(node);
                if (!course.isBlank()) {
                    courses.add(course);
                }
            }
        }
        return new ArrayList<>(courses);
    }

    /**
     * 推导节点所属课程：优先节点 course 字段，否则取规范化 module
     */
    private String resolveCourse(KnowledgeNode node) {
        if (node.getCourse() != null && !node.getCourse().isBlank()) {
            return node.getCourse();
        }
        return normalizeModuleName(node.getModule());
    }

    /**
     * 推导节点适配专业：优先节点 majorDirection 字段，否则按课程映射
     */
    private String resolveMajor(KnowledgeNode node) {
        if (node.getMajorDirection() != null && !node.getMajorDirection().isBlank()) {
            return node.getMajorDirection();
        }
        return COURSE_MAJOR.getOrDefault(resolveCourse(node), "");
    }

    /**
     * 返回所有模块名称
     */
    public Set<String> getAllModules() {
        return nodeMap.values().stream()
                .map(KnowledgeNode::getModule)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * 总节点数
     */
    public int getNodeCount() {
        return nodeMap.size();
    }

    /**
     * 判断节点是否为叶子节点（没有其他节点依赖它）
     */
    private boolean isLeafNode(String id) {
        return nodeMap.values().stream()
                .noneMatch(node -> node.getPrerequisites() != null && node.getPrerequisites().contains(id));
    }

    /**
     * 创建节点副本，防止外部修改内部状态
     */
    private KnowledgeNode copyNode(KnowledgeNode original) {
        KnowledgeNode copy = new KnowledgeNode();
        copy.setId(original.getId());
        copy.setName(original.getName());
        copy.setModule(original.getModule());
        copy.setDifficulty(original.getDifficulty());
        copy.setPrerequisites(original.getPrerequisites() != null
                ? new ArrayList<>(original.getPrerequisites())
                : null);
        copy.setErrorTypes(original.getErrorTypes() != null
                ? new ArrayList<>(original.getErrorTypes())
                : null);
        copy.setCommonErrors(original.getCommonErrors() != null
                ? new ArrayList<>(original.getCommonErrors())
                : null);
        copy.setDescription(original.getDescription());
        copy.setExamWeight(original.getExamWeight());
        copy.setEstimatedMinutes(original.getEstimatedMinutes());
        copy.setTeachingTips(original.getTeachingTips());
        copy.setCourse(original.getCourse());
        copy.setMajorDirection(original.getMajorDirection());
        copy.setEducationLevels(original.getEducationLevels() != null
                ? new ArrayList<>(original.getEducationLevels())
                : null);
        copy.setResourceTypes(original.getResourceTypes() != null
                ? new ArrayList<>(original.getResourceTypes())
                : null);
        return copy;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KnowledgeNode {
        private String id;
        private String name;
        private String module;
        private double difficulty;
        private List<String> prerequisites;
        private List<String> errorTypes;
        private List<String> commonErrors;
        private String description;
        private double examWeight;
        private Integer estimatedMinutes;
        private String teachingTips;
        /** 所属课程，缺省时由 module 推导 */
        private String course;
        /** 适配的专业方向，缺省时由课程推导 */
        private String majorDirection;
        /** 适配的学历层次列表，为空表示不限 */
        private List<String> educationLevels;
        /** 推荐生成的资源类型 */
        private List<String> resourceTypes;
    }
}
