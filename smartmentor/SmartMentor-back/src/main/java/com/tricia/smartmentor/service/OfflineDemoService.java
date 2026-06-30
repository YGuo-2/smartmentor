package com.tricia.smartmentor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class OfflineDemoService {

    private final ObjectMapper objectMapper;

    public String buildAgentResponse(String systemPrompt, String userMessage) {
        Map<String, Object> response;
        String system = systemPrompt != null ? systemPrompt : "";
        String user = userMessage != null ? userMessage : "";

        if (system.contains("诊断Agent")) {
            response = user.contains("分析") ? diagnosticAnalysis() : diagnosticQuestions();
        } else if (system.contains("知识追踪专家")) {
            response = tracingResult();
        } else if (system.contains("课程规划专家")) {
            response = planningResult();
        } else if (system.contains("教学Agent")) {
            response = teachingContent();
        } else if (system.contains("评估Agent")) {
            response = user.contains("生成检测题") ? checkpointQuestions() : evaluationResult();
        } else {
            response = genericJson();
        }

        return toJson(response);
    }

    public String buildChatResponse(List<Map<String, String>> messages) {
        String latest = messages == null || messages.isEmpty()
                ? "当前问题"
                : messages.get(messages.size() - 1).getOrDefault("content", "当前问题");
        return "【离线演示模式】我已收到你的问题：" + latest
                + "\n\n建议先结合专业方向和当前课程定位对应知识点，再查看诊断结果和学习路径中的薄弱项。";
    }

    private Map<String, Object> diagnosticQuestions() {
        List<Map<String, Object>> questions = new ArrayList<>();
        String[] kps = {"ai_intro", "ai_search", "ai_knowledge_representation", "ai_ml_basic",
                "jw_http_basic", "jw_spring_boot", "dc_logic_gate", "dc_boolean_algebra"};
        for (int i = 0; i < 8; i++) {
            Map<String, Object> question = new LinkedHashMap<>();
            question.put("id", "q" + (i + 1));
            question.put("knowledgePointId", kps[i]);
            question.put("question", "离线演示题" + (i + 1) + "：根据知识点完成一个基础判断，正确选项是哪一项？");
            question.put("options", Map.of("A", "干扰项A", "B", "正确结论", "C", "干扰项C", "D", "干扰项D"));
            question.put("correctAnswer", "B");
            question.put("difficulty", Math.min(5, 2 + i % 4));
            question.put("errorType", i % 2 == 0 ? "概念理解错误" : "实践迁移不足");
            question.put("explanation", "离线演示解析：先识别课程概念、适用场景和关键约束，再排除不符合条件的选项。");
            questions.add(question);
        }
        return Map.of("questions", questions);
    }

    private Map<String, Object> diagnosticAnalysis() {
        return Map.of(
                "score", 68,
                "weakPoints", List.of(
                        Map.of("kpId", "ai_knowledge_representation", "kpName", "知识表示与推理", "masteryLevel", 0.42),
                        Map.of("kpId", "jw_rest_api", "kpName", "REST API 设计", "masteryLevel", 0.35)
                ),
                "errorPatterns", Map.of("概念理解错误", 2, "实践迁移不足", 1),
                "detailedAnalysis", List.of(
                        Map.of("questionId", "q2", "correct", false, "errorType", "概念理解错误",
                                "analysis", "事实、规则和推理结论的边界不清"),
                        Map.of("questionId", "q5", "correct", false, "errorType", "实践迁移不足",
                                "analysis", "接口设计场景中资源路径和请求方法对应关系不稳定")
                )
        );
    }

    private Map<String, Object> tracingResult() {
        return Map.of(
                "rootCauses", List.of(
                        Map.of("nodeId", "ai_intro", "reason", "人工智能整体框架不稳导致知识表示任务边界混淆", "confidence", 0.86),
                        Map.of("nodeId", "jw_http_basic", "reason", "HTTP 请求语义不清影响 REST API 设计判断", "confidence", 0.78)
                ),
                "masteryEstimates", Map.of("ai_intro", 0.46, "jw_http_basic", 0.52),
                "crossModuleLinks", List.of("jw_rest_api"),
                "analysisNarrative", "主要薄弱点来自课程整体框架和 Web 协议基础。建议先补人工智能任务分类与 HTTP 语义，再进入知识表示和接口设计训练。"
        );
    }

    private Map<String, Object> planningResult() {
        return Map.of(
                "learningPath", List.of(
                        Map.of("nodeId", "ai_intro", "estimatedMinutes", 30, "priority", "high", "reason", "补齐人工智能任务与边界认知"),
                        Map.of("nodeId", "ai_knowledge_representation", "estimatedMinutes", 45, "priority", "high", "reason", "强化事实、规则与推理表达"),
                        Map.of("nodeId", "jw_http_basic", "estimatedMinutes", 35, "priority", "medium", "reason", "衔接 Web 请求响应基础"),
                        Map.of("nodeId", "jw_rest_api", "estimatedMinutes", 50, "priority", "medium", "reason", "完成 REST API 设计训练")
                ),
                "totalEstimatedHours", 1.5,
                "milestones", List.of(
                        Map.of("afterNode", "ai_knowledge_representation", "checkpointType", "quick_quiz", "description", "知识表示专项检测"),
                        Map.of("afterNode", "jw_rest_api", "checkpointType", "quick_quiz", "description", "接口设计检测")
                ),
                "adaptiveNotes", "离线演示路径：先补课程基础框架，再推进专业实践能力。"
        );
    }

    private Map<String, Object> teachingContent() {
        return Map.of(
                "title", "知识表示与推理专项讲解",
                "strategy", "targeted",
                "conceptExplanation", Map.of(
                        "plainLanguage", "知识表示就是把现实问题中的事实、关系和规则整理成机器可以处理的结构。",
                        "formalDefinition", "常见形式包括命题逻辑、谓词逻辑、产生式规则和知识图谱三元组。",
                        "visualDescription", "可以把课程选课关系表示成“学生-选择-课程”“课程-需要-先修课”等三元组。"
                ),
                "examples", List.of(
                        Map.of("title", "三元组建模", "question", "如何表示“Java Web 需要 HTTP 基础”？", "solution", "可表示为（Java Web 开发，先修，HTTP 与 Web 基础）")
                ),
                "exercises", List.of(
                        Map.of("exerciseId", "ex1", "question", "知识图谱中三元组通常由哪三部分构成？", "correctAnswer", "主体、关系、客体", "difficulty", 0.3,
                                "explanation", "三元组用于表达实体之间的关系。"),
                        Map.of("exerciseId", "ex2", "question", "规则“如果学生未掌握 HTTP，则推荐 HTTP 基础”属于事实还是规则？", "correctAnswer", "规则", "difficulty", 0.4,
                                "explanation", "它描述了条件和动作之间的推理关系。")
                ),
                "commonMistakes", List.of("把事实和规则混在一起", "三元组方向写反"),
                "summary", "先识别实体，再定义关系，最后补充可推理的规则。"
        );
    }

    private Map<String, Object> checkpointQuestions() {
        return Map.of("checkpointQuestions", List.of(
                Map.of("id", "cp1", "question", "知识图谱三元组最核心的结构是什么？",
                        "options", Map.of("A", "变量、类型、对象", "B", "主体、关系、客体", "C", "请求、响应、状态码", "D", "输入、输出、时钟"),
                        "correctAnswer", "B", "difficulty", 1, "explanation", "三元组用于表达实体与实体之间的关系。"),
                Map.of("id", "cp2", "question", "REST API 中查询某个课程资源详情更适合使用哪类路径？",
                        "options", Map.of("A", "/courses/{id}", "B", "/doCourse", "C", "/saveEverything", "D", "/page1"),
                        "correctAnswer", "A", "difficulty", 2, "explanation", "REST 风格强调资源路径和 HTTP 方法语义。")
        ));
    }

    private Map<String, Object> evaluationResult() {
        return Map.of(
                "overallAssessment", "学生已掌握基础概念识别，但跨场景建模和接口设计仍需训练。",
                "masteryEstimate", 0.74,
                "strengths", List.of("能区分事实与关系", "能识别基础资源路径"),
                "weaknesses", List.of("规则表达和接口语义迁移不够稳定"),
                "suggestion", "继续完成2组知识图谱建模和 REST API 设计练习。",
                "detailedResults", List.of(
                        Map.of("questionId", "cp1", "correct", true, "errorType", "", "analysis", "基础概念掌握良好")
                )
        );
    }

    private Map<String, Object> genericJson() {
        return Map.of("message", "offline demo response", "fallbackUsed", true);
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }
}
