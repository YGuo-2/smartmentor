package com.tricia.smartmentor.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tricia.smartmentor.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识追踪与根因分析 Agent。
 * <p>
 * 接收诊断阶段识别出的薄弱点和错误模式，结合知识图谱信息，
 * 像医生诊断病因一样追溯到学生知识薄弱的根本原因知识点。
 */
@Slf4j
@Component
public class TracingAgent extends BaseAgent {

    public TracingAgent(LlmService llmService, ObjectMapper objectMapper) {
        super(llmService, objectMapper);
    }

    @Override
    protected String getName() {
        return "溯源Agent";
    }

    @Override
    protected double getTemperature() {
        return 0.3;
    }

    @Override
    protected String getSystemPrompt() {
        String fallback = "你是知识追踪专家，需要像医生诊断病因一样找到学生知识薄弱的根本原因。"
                + "你擅长分析学生的错误模式，结合知识图谱中的依赖关系，追溯到最底层的知识缺陷。"
                + "你的分析必须精确、有据可依，每个根因判断都需要给出置信度。"
                + "请严格按照要求的JSON格式输出结果。";
        return loadPromptTemplate(getPromptTemplateKey(), "tracing-system-v1", fallback).getContent();
    }

    @Override
    protected String getPromptTemplateKey() {
        return "tracing-system";
    }

    @Override
    protected String callLLM(String prompt, AgentContext context) {
        return llmService.chatJsonSync(prompt, getSystemPrompt(), getTemperature());
    }

    @Override
    protected String buildPrompt(AgentContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("## 任务：知识追踪与根因分析\n\n");

        // 薄弱点信息
        prompt.append("### 薄弱点\n");
        if (context.getWeakPoints() != null && !context.getWeakPoints().isEmpty()) {
            for (Map<String, Object> wp : context.getWeakPoints()) {
                prompt.append("- ").append(wp.get("kpId"))
                        .append("(").append(wp.get("kpName")).append(")")
                        .append(" 掌握度:").append(wp.get("masteryLevel"))
                        .append("\n");
            }
        } else {
            prompt.append("暂无\n");
        }

        // 错误模式
        if (context.getErrorPatterns() != null && !context.getErrorPatterns().isEmpty()) {
            prompt.append("\n### 错误模式\n");
            for (Map.Entry<String, Integer> entry : context.getErrorPatterns().entrySet()) {
                prompt.append("- ").append(entry.getKey()).append(":").append(entry.getValue()).append("次\n");
            }
        }

        // 知识图谱信息
        Object knowledgeGraph = context.getSessionData().get("knowledgeGraph");
        if (knowledgeGraph != null) {
            prompt.append("\n### 知识图谱\n").append(knowledgeGraph.toString()).append("\n");
        }

        prompt.append("\n### 输出要求\n");
        prompt.append("返回JSON：{\"rootCauses\":[{\"nodeId\":\"知识点ID\",\"reason\":\"原因\",\"confidence\":0.85}],");
        prompt.append("\"masteryEstimates\":{\"知识点ID\":掌握概率},");
        prompt.append("\"crossModuleLinks\":[\"跨模块关联知识点ID\"],");
        prompt.append("\"analysisNarrative\":\"整体分析（2-3句话）\"}\n");
        prompt.append("rootCauses必须从知识图谱前置节点中选取，analysisNarrative简洁。\n");

        return prompt.toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, Object> parseResponse(String llmResponse, AgentContext context) {
        Map<String, Object> parsed = safeParseJson(llmResponse);

        // 提取rootCauses并设置到context
        List<Map<String, Object>> rootCausesList = (List<Map<String, Object>>) parsed.get("rootCauses");
        if (rootCausesList != null && !rootCausesList.isEmpty()) {
            List<String> rootCauseIds = new ArrayList<>();
            for (Map<String, Object> cause : rootCausesList) {
                Object nodeId = cause.get("nodeId");
                if (nodeId != null) {
                    rootCauseIds.add(nodeId.toString());
                }
            }
            context.setRootCauses(rootCauseIds);
        }

        // 更新knowledgeMastery
        Map<String, Object> masteryEstimates = (Map<String, Object>) parsed.get("masteryEstimates");
        if (masteryEstimates != null) {
            for (Map.Entry<String, Object> entry : masteryEstimates.entrySet()) {
                try {
                    double mastery = Double.parseDouble(entry.getValue().toString());
                    context.updateMastery(entry.getKey(), mastery);
                } catch (NumberFormatException e) {
                    log.warn("[{}] 掌握度解析失败: key={}, value={}", getName(), entry.getKey(), entry.getValue());
                }
            }
        }

        return parsed;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected AgentResponse qualityCheck(Map<String, Object> parsed, AgentContext context) {
        // rootCauses不能为空
        List<String> rootCauses = context.getRootCauses();
        if (rootCauses == null || rootCauses.isEmpty()) {
            return AgentResponse.failure("根因分析失败：未能识别出任何根本原因知识点");
        }

        // 判断是否存在跨模块关联
        List<String> crossModuleLinks = (List<String>) parsed.get("crossModuleLinks");
        AgentEvent event;
        if (crossModuleLinks != null && !crossModuleLinks.isEmpty()) {
            event = AgentEvent.CROSS_MODULE_ROOT_FOUND;
            log.info("[{}] 发现跨模块根因关联: {}", getName(), crossModuleLinks);
        } else {
            event = AgentEvent.TRACING_COMPLETE;
        }

        String message = String.format("根因分析完成，识别出%d个根本原因知识点", rootCauses.size());
        return AgentResponse.success(message, parsed, event);
    }
}
