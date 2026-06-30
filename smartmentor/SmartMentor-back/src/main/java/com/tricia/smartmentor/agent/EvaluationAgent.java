package com.tricia.smartmentor.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tricia.smartmentor.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 评估 Agent —— 生成检测题并评估学生掌握度。
 * <p>
 * 支持两种动作（通过 {@code context.sessionData.get("action")} 区分）：
 * <ul>
 *   <li><b>generate_checkpoint</b> — 生成 3~5 道难度递进的检测题</li>
 *   <li><b>evaluate</b> — 分析答题结果，用 BKT 模型更新掌握度</li>
 * </ul>
 * <p>
 * BKT 简化模型参数：
 * <ul>
 *   <li>P(L0) = 先验掌握概率（来自 context.knowledgeMastery）</li>
 *   <li>P(T) = 0.3 转移概率（学习后掌握概率）</li>
 *   <li>P(G) = 0.1 猜测概率</li>
 *   <li>P(S) = 0.15 失误概率</li>
 * </ul>
 */
@Slf4j
@Component
public class EvaluationAgent extends BaseAgent {

    // BKT 模型参数
    private static final double P_TRANSIT = 0.3;
    private static final double P_GUESS = 0.1;
    private static final double P_SLIP = 0.15;
    private static final double MASTERY_THRESHOLD = 0.8;

    public EvaluationAgent(LlmService llmService, ObjectMapper objectMapper) {
        super(llmService, objectMapper);
    }

    @Override
    protected String getName() {
        return "评估Agent";
    }

    @Override
    protected double getTemperature() {
        return 0.5;
    }

    @Override
    protected String getSystemPrompt() {
        String fallback = "你是SmartMentor智学导师系统的评估Agent，专门负责高校课程知识点掌握度检测和学习效果评估。" +
                "你需要根据学生的专业方向、学历层次和学习目标生成有梯度的检测题，精确评估学生对特定知识点或实践能力的掌握程度。" +
                "请严格按照要求的JSON格式输出结果，不要附加任何额外文字。";
        return loadPromptTemplate(getPromptTemplateKey(), "evaluation-system-v1", fallback).getContent();
    }

    @Override
    protected String getPromptTemplateKey() {
        return "evaluation-system";
    }

    @Override
    protected String callLLM(String prompt, AgentContext context) {
        String action = resolveAction(context);
        double temperature = "evaluate".equals(action) ? 0.3 : 0.5;
        return llmService.chatSync(prompt, getSystemPrompt(), temperature);
    }

    @Override
    protected String buildPrompt(AgentContext context) {
        String action = resolveAction(context);
        if ("evaluate".equals(action)) {
            return buildEvaluatePrompt(context);
        }
        return buildCheckpointPrompt(context);
    }

    @Override
    protected Map<String, Object> parseResponse(String llmResponse, AgentContext context) {
        String action = resolveAction(context);
        if ("evaluate".equals(action)) {
            return parseEvaluateResponse(llmResponse, context);
        }
        return parseCheckpointResponse(llmResponse, context);
    }

    @Override
    protected AgentResponse qualityCheck(Map<String, Object> parsed, AgentContext context) {
        String action = resolveAction(context);
        if ("evaluate".equals(action)) {
            return qualityCheckEvaluate(parsed, context);
        }
        return qualityCheckCheckpoint(parsed, context);
    }

    // ================================================================== checkpoint 生成

    @SuppressWarnings("unchecked")
    private String buildCheckpointPrompt(AgentContext context) {
        Map<String, Object> sessionData = context.getSessionData();

        String kpId = (String) sessionData.getOrDefault("knowledgePointId", "");
        String kpName = (String) sessionData.getOrDefault("knowledgePointName", "未知知识点");
        double masteryLevel = 0.5;
        Object mlObj = sessionData.get("masteryLevel");
        if (mlObj instanceof Number) {
            masteryLevel = ((Number) mlObj).doubleValue();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## 任务\n");
        sb.append("请为以下知识点生成检测题，用于评估学生是否已掌握。\n\n");

        sb.append("## 知识点信息\n");
        sb.append("- ID：").append(kpId).append("\n");
        sb.append("- 名称：").append(kpName).append("\n");
        sb.append("- 所属模块：").append(context.getModule()).append("\n");
        sb.append("- 当前估计掌握度：").append(String.format("%.2f", masteryLevel)).append("\n\n");

        sb.append("## 输出要求\n");
        sb.append("请以JSON格式返回，结构如下：\n");
        sb.append("```json\n");
        sb.append("{\n");
        sb.append("  \"checkpointQuestions\": [\n");
        sb.append("    {\n");
        sb.append("      \"id\": \"cp1\",\n");
        sb.append("      \"question\": \"题目内容\",\n");
        sb.append("      \"options\": {\"A\": \"选项A\", \"B\": \"选项B\", \"C\": \"选项C\", \"D\": \"选项D\"},\n");
        sb.append("      \"correctAnswer\": \"B\",\n");
        sb.append("      \"difficulty\": 2,\n");
        sb.append("      \"explanation\": \"详细解析\"\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n");
        sb.append("```\n\n");
        sb.append("要求：\n");
        sb.append("1. 生成4道检测题，难度递进（1=基础, 2=简单, 3=中等, 4=较难, 5=困难）\n");
        sb.append("2. 所有题目都围绕该知识点\n");
        sb.append("3. correctAnswer 只填 A/B/C/D 中的一个字母\n");

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseCheckpointResponse(String llmResponse, AgentContext context) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> json = safeParseJson(llmResponse);

            Object cpObj = json.get("checkpointQuestions");
            if (cpObj instanceof List) {
                result.put("checkpointQuestions", cpObj);
                result.put("questionCount", ((List<?>) cpObj).size());
            } else {
                result.put("checkpointQuestions", Collections.emptyList());
                result.put("questionCount", 0);
            }
        } catch (Exception e) {
            log.error("[{}] 解析检测题失败: {}", getName(), e.getMessage());
            result.put("checkpointQuestions", Collections.emptyList());
            result.put("questionCount", 0);
        }
        return result;
    }

    private AgentResponse qualityCheckCheckpoint(Map<String, Object> parsed, AgentContext context) {
        int count = 0;
        Object countObj = parsed.get("questionCount");
        if (countObj instanceof Number) {
            count = ((Number) countObj).intValue();
        }

        if (count < 2) {
            return AgentResponse.failure("检测题数量不足");
        }

        context.putSessionData("checkpointQuestions", parsed.get("checkpointQuestions"));

        return AgentResponse.success(
                String.format("生成 %d 道检测题", count),
                parsed);
    }

    // ================================================================== evaluate 评估

    @SuppressWarnings("unchecked")
    private String buildEvaluatePrompt(AgentContext context) {
        Map<String, Object> sessionData = context.getSessionData();

        String kpId = (String) sessionData.getOrDefault("knowledgePointId", "");
        String kpName = (String) sessionData.getOrDefault("knowledgePointName", "未知知识点");

        List<Map<String, Object>> checkpointQuestions = Collections.emptyList();
        Object cpObj = sessionData.get("checkpointQuestions");
        if (cpObj instanceof List) {
            checkpointQuestions = (List<Map<String, Object>>) cpObj;
        }

        Map<String, String> answers = Collections.emptyMap();
        Object aObj = sessionData.get("checkpointAnswers");
        if (aObj instanceof Map) {
            answers = (Map<String, String>) aObj;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## 任务\n");
        sb.append("请分析学生的检测题作答结果，评估其对知识点的掌握程度。\n\n");

        sb.append("## 知识点：").append(kpName).append(" (").append(kpId).append(")\n\n");

        sb.append("## 检测结果\n");
        int correctCount = 0;
        for (Map<String, Object> q : checkpointQuestions) {
            String qId = String.valueOf(q.get("id"));
            String studentAnswer = answers.getOrDefault(qId, "未作答");
            String correctAnswer = String.valueOf(q.get("correctAnswer"));
            boolean isCorrect = studentAnswer.equalsIgnoreCase(correctAnswer);
            if (isCorrect) correctCount++;

            sb.append("### ").append(qId).append("\n");
            sb.append("- 题目：").append(q.get("question")).append("\n");
            sb.append("- 正确答案：").append(correctAnswer).append("\n");
            sb.append("- 学生答案：").append(studentAnswer).append("\n");
            sb.append("- 结果：").append(isCorrect ? "✓ 正确" : "✗ 错误").append("\n");
            sb.append("- 难度：").append(q.get("difficulty")).append("\n\n");
        }

        sb.append("## 输出要求\n");
        sb.append("请以JSON格式返回，结构如下：\n");
        sb.append("```json\n");
        sb.append("{\n");
        sb.append("  \"overallAssessment\": \"整体评估描述\",\n");
        sb.append("  \"masteryEstimate\": 0.7,\n");
        sb.append("  \"strengths\": [\"掌握的方面1\"],\n");
        sb.append("  \"weaknesses\": [\"不足之处1\"],\n");
        sb.append("  \"suggestion\": \"后续学习建议\",\n");
        sb.append("  \"detailedResults\": [\n");
        sb.append("    {\n");
        sb.append("      \"questionId\": \"cp1\",\n");
        sb.append("      \"correct\": true,\n");
        sb.append("      \"errorType\": \"如有错误的类型\",\n");
        sb.append("      \"analysis\": \"分析\"\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n");
        sb.append("```\n");

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseEvaluateResponse(String llmResponse, AgentContext context) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> json = safeParseJson(llmResponse);

            result.put("overallAssessment", json.getOrDefault("overallAssessment", ""));
            result.put("strengths", json.getOrDefault("strengths", Collections.emptyList()));
            result.put("weaknesses", json.getOrDefault("weaknesses", Collections.emptyList()));
            result.put("suggestion", json.getOrDefault("suggestion", ""));
            result.put("detailedResults", json.getOrDefault("detailedResults", Collections.emptyList()));

            // LLM给出的主观掌握度估计
            double llmMastery = 0.5;
            Object meObj = json.get("masteryEstimate");
            if (meObj instanceof Number) {
                llmMastery = ((Number) meObj).doubleValue();
            }
            result.put("llmMasteryEstimate", llmMastery);

            // BKT 模型更新掌握度
            Map<String, Object> sessionData = context.getSessionData();
            String kpId = (String) sessionData.getOrDefault("knowledgePointId", "");

            // 统计正确率
            Map<String, String> answers = Collections.emptyMap();
            Object aObj = sessionData.get("checkpointAnswers");
            if (aObj instanceof Map) {
                answers = (Map<String, String>) aObj;
            }

            List<Map<String, Object>> questions = Collections.emptyList();
            Object cpObj = sessionData.get("checkpointQuestions");
            if (cpObj instanceof List) {
                questions = (List<Map<String, Object>>) cpObj;
            }

            // 获取先验掌握概率
            double priorMastery = context.getKnowledgeMastery().getOrDefault(kpId, 0.5);

            // 逐题BKT更新
            double pL = priorMastery;
            for (Map<String, Object> q : questions) {
                String qId = String.valueOf(q.get("id"));
                String studentAnswer = answers.getOrDefault(qId, "");
                String correctAnswer = String.valueOf(q.get("correctAnswer"));
                boolean isCorrect = studentAnswer.equalsIgnoreCase(correctAnswer);

                pL = bktUpdate(pL, isCorrect);
            }

            // 综合BKT和LLM估计（7:3权重）
            double finalMastery = Math.min(1.0, Math.max(0.0, pL * 0.7 + llmMastery * 0.3));
            result.put("bktMastery", pL);
            result.put("finalMastery", finalMastery);
            result.put("passed", finalMastery >= MASTERY_THRESHOLD);

            // 回写 context
            context.updateMastery(kpId, finalMastery);

        } catch (Exception e) {
            log.error("[{}] 解析评估结果失败: {}", getName(), e.getMessage());
        }
        return result;
    }

    private AgentResponse qualityCheckEvaluate(Map<String, Object> parsed, AgentContext context) {
        boolean passed = Boolean.TRUE.equals(parsed.get("passed"));
        double finalMastery = 0.0;
        Object fmObj = parsed.get("finalMastery");
        if (fmObj instanceof Number) {
            finalMastery = ((Number) fmObj).doubleValue();
        }

        AgentEvent event = passed ? AgentEvent.MASTERY_REACHED : AgentEvent.MASTERY_NOT_REACHED;
        String message = String.format("评估完成：掌握度 %.0f%%，%s",
                finalMastery * 100,
                passed ? "已达标 ✓" : "未达标，需要继续学习");

        return AgentResponse.success(message, parsed, event);
    }

    // ================================================================== BKT 模型

    /**
     * BKT 单步更新。
     * <pre>
     * 如果答对：P(L|correct) = P(L)*(1-P(S)) / [P(L)*(1-P(S)) + (1-P(L))*P(G)]
     * 如果答错：P(L|wrong)   = P(L)*P(S) / [P(L)*P(S) + (1-P(L))*(1-P(G))]
     * 转移更新：P(L_new) = P(L|obs) + (1-P(L|obs))*P(T)
     * </pre>
     */
    private double bktUpdate(double pL, boolean correct) {
        double pLGivenObs;
        if (correct) {
            double numerator = pL * (1 - P_SLIP);
            double denominator = pL * (1 - P_SLIP) + (1 - pL) * P_GUESS;
            pLGivenObs = denominator > 0 ? numerator / denominator : pL;
        } else {
            double numerator = pL * P_SLIP;
            double denominator = pL * P_SLIP + (1 - pL) * (1 - P_GUESS);
            pLGivenObs = denominator > 0 ? numerator / denominator : pL;
        }
        return pLGivenObs + (1 - pLGivenObs) * P_TRANSIT;
    }

    private String resolveAction(AgentContext context) {
        Object action = context.getSessionData().get("action");
        if (action instanceof String) {
            return (String) action;
        }
        return "generate_checkpoint";
    }
}
