package com.tricia.smartmentor.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tricia.smartmentor.service.LlmService;
import com.tricia.smartmentor.service.KnowledgeGraphService;
import com.tricia.smartmentor.service.KnowledgeGraphService.KnowledgeNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 个性化学习路径规划 Agent。
 * <p>
 * 基于根因分析结果和知识图谱拓扑信息，生成从根因知识点开始、
 * 按依赖关系逐步推进到目标知识点的最优学习路径。
 * 遵循"先补基础再进阶"原则。
 */
@Slf4j
@Component
public class PlanningAgent extends BaseAgent {

    private final KnowledgeGraphService knowledgeGraphService;

    public PlanningAgent(LlmService llmService,
                         ObjectMapper objectMapper,
                         KnowledgeGraphService knowledgeGraphService) {
        super(llmService, objectMapper);
        this.knowledgeGraphService = knowledgeGraphService;
    }

    @Override
    protected String getName() {
        return "规划Agent";
    }

    @Override
    protected double getTemperature() {
        return 0.3;
    }

    @Override
    protected String callLLM(String prompt, AgentContext context) {
        // 规划要求严格的 learningPath/milestones JSON，与诊断/溯源 Agent 对齐启用 json 模式
        return llmService.chatJsonSync(prompt, getSystemPrompt(), getTemperature());
    }

    @Override
    protected String getSystemPrompt() {
        String fallback = "你是课程规划专家，需要设计最优学习路径，遵循「先补基础再进阶」原则。"
                + "你擅长根据知识点间的依赖关系和学生当前掌握情况，规划出高效、循序渐进的学习计划。"
                + "路径设计需要考虑学生的认知负荷，合理安排学习时间和检查点。"
                + "请严格按照要求的JSON格式输出结果。";
        return loadPromptTemplate(getPromptTemplateKey(), "planning-system-v1", fallback).getContent();
    }

    @Override
    protected String getPromptTemplateKey() {
        return "planning-system";
    }

    @Override
    protected String buildPrompt(AgentContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("## 任务：生成个性化学习路径\n\n");

        // 根因知识点
        prompt.append("### 根因分析结果\n");
        if (context.getRootCauses() != null && !context.getRootCauses().isEmpty()) {
            prompt.append("根本原因知识点（按重要性降序）：\n");
            for (String rootCause : context.getRootCauses()) {
                prompt.append("- ").append(rootCause).append("\n");
            }
        } else {
            prompt.append("暂无根因数据\n");
        }

        // 知识图谱拓扑序
        prompt.append("\n### 知识图谱拓扑信息\n");
        Object topologicalOrder = context.getSessionData().get("topologicalOrder");
        if (topologicalOrder != null) {
            prompt.append("知识点拓扑排序（学习先后顺序）：\n");
            prompt.append(topologicalOrder.toString()).append("\n");
        } else {
            prompt.append("暂无拓扑排序数据，请基于教学经验安排合理顺序\n");
        }

        // 当前掌握度
        prompt.append("\n### 学生当前掌握程度\n");
        if (context.getKnowledgeMastery() != null && !context.getKnowledgeMastery().isEmpty()) {
            for (Map.Entry<String, Double> entry : context.getKnowledgeMastery().entrySet()) {
                prompt.append("- ").append(entry.getKey()).append(": ")
                        .append(String.format("%.2f", entry.getValue())).append("\n");
            }
        } else {
            prompt.append("暂无掌握度数据\n");
        }

        // 薄弱点信息
        prompt.append("\n### 学生薄弱点\n");
        if (context.getWeakPoints() != null && !context.getWeakPoints().isEmpty()) {
            for (Map<String, Object> wp : context.getWeakPoints()) {
                prompt.append("- 知识点ID: ").append(wp.get("kpId"))
                        .append(", 名称: ").append(wp.get("kpName"))
                        .append(", 掌握度: ").append(wp.get("masteryLevel"))
                        .append("\n");
            }
        }

        prompt.append("\n### 要求\n");
        prompt.append("请基于以上信息，设计从根因知识点出发、按依赖关系逐步推进到目标知识点的学习路径。\n");
        prompt.append("目标：从根因开始，按依赖关系逐步推进到目标知识点，先补基础再进阶。\n");
        prompt.append("返回以下JSON格式：\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"learningPath\": [\n");
        prompt.append("    {\"nodeId\": \"知识点ID\", \"estimatedMinutes\": 15, \"priority\": \"high\", \"reason\": \"学习该节点的原因\"},\n");
        prompt.append("    {\"nodeId\": \"知识点ID\", \"estimatedMinutes\": 20, \"priority\": \"medium\", \"reason\": \"学习该节点的原因\"}\n");
        prompt.append("  ],\n");
        prompt.append("  \"totalEstimatedHours\": 3.5,\n");
        prompt.append("  \"milestones\": [\n");
        prompt.append("    {\"afterNode\": \"知识点ID\", \"checkpointType\": \"quick_quiz\", \"description\": \"检查点描述\"}\n");
        prompt.append("  ],\n");
        prompt.append("  \"adaptiveNotes\": \"个性化学习建议\"\n");
        prompt.append("}\n");
        prompt.append("```\n");

        return prompt.toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, Object> parseResponse(String llmResponse, AgentContext context) {
        Map<String, Object> parsed = safeParseJson(llmResponse);

        // 提取learningPath的nodeId列表设置到context
        List<Map<String, Object>> learningPathList = (List<Map<String, Object>>) parsed.get("learningPath");
        if (learningPathList != null && !learningPathList.isEmpty()) {
            List<String> pathNodeIds = new ArrayList<>();
            for (Map<String, Object> pathItem : learningPathList) {
                Object nodeId = pathItem.get("nodeId");
                if (nodeId != null) {
                    pathNodeIds.add(nodeId.toString());
                }
            }
            context.setLearningPath(pathNodeIds);
        }

        return parsed;
    }

    @Override
    protected AgentResponse qualityCheck(Map<String, Object> parsed, AgentContext context) {
        // learningPath至少3个节点
        List<String> learningPath = context.getLearningPath();
        if (learningPath == null || learningPath.size() < 3) {
            int actual = (learningPath == null) ? 0 : learningPath.size();
            return AgentResponse.failure(
                    String.format("学习路径规划不足：至少需要3个节点，当前仅%d个", actual));
        }

        Map<String, Integer> positionByNodeId = new HashMap<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < learningPath.size(); i++) {
            String nodeId = learningPath.get(i) != null ? learningPath.get(i).trim() : "";
            if (nodeId.isBlank()) {
                return AgentResponse.failure("学习路径规划无效：存在空知识点ID");
            }
            if (!seen.add(nodeId)) {
                return AgentResponse.failure("学习路径规划无效：存在重复知识点 " + nodeId);
            }
            KnowledgeNode node = knowledgeGraphService.getNode(nodeId);
            if (node == null) {
                return AgentResponse.failure("学习路径规划无效：知识点不在知识图谱中 " + nodeId);
            }
            learningPath.set(i, nodeId);
            positionByNodeId.put(nodeId, i);
        }

        for (String nodeId : learningPath) {
            KnowledgeNode node = knowledgeGraphService.getNode(nodeId);
            List<String> prerequisites = node.getPrerequisites();
            if (prerequisites == null || prerequisites.isEmpty()) {
                continue;
            }
            int nodePosition = positionByNodeId.get(nodeId);
            for (String prerequisiteId : prerequisites) {
                Integer prerequisitePosition = positionByNodeId.get(prerequisiteId);
                if (prerequisitePosition != null && prerequisitePosition > nodePosition) {
                    return AgentResponse.failure(String.format(
                            "学习路径规划无效：前置知识点顺序倒置，%s 应早于 %s",
                            prerequisiteId, nodeId));
                }
            }
        }

        String message = String.format("学习路径生成完成，共%d个知识点节点", learningPath.size());
        return AgentResponse.success(message, parsed, AgentEvent.PATH_GENERATED);
    }
}
