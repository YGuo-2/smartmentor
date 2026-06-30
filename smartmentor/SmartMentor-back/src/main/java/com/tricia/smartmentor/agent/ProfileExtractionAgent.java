package com.tricia.smartmentor.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tricia.smartmentor.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 画像构建 Agent：从一段自然语言对话中抽取学生画像特征，输出严格 JSON。
 * <p>
 * 用于「对话式学习画像自主构建」——摒弃表单，通过引导访谈 / 日常对话自动抽取
 * 专业方向、学习目标、认知风格、基础水平、资源偏好、薄弱模块等画像维度。
 * <p>
 * 强约束防幻觉：无法从对话推断的字段一律留空（空串 / 空数组），禁止编造。
 */
@Slf4j
@Component
public class ProfileExtractionAgent extends BaseAgent {

    public ProfileExtractionAgent(LlmService llmService, ObjectMapper objectMapper) {
        super(llmService, objectMapper);
    }

    @Override
    protected String getName() {
        return "画像构建Agent";
    }

    @Override
    protected double getTemperature() {
        // 结构化抽取，低温保证稳定与精确
        return 0.2;
    }

    @Override
    protected String getSystemPrompt() {
        return "你是 SmartMentor 的学生画像抽取器。你的唯一职责是从给定的对话内容中，"
                + "抽取学生的学习画像特征，并只输出严格的 JSON。不要输出任何解释、Markdown 或多余文字。"
                + "严禁编造：凡是对话里没有明确依据的字段，一律返回空字符串或空数组。";
    }

    @Override
    protected String callLLM(String prompt, AgentContext context) {
        // 用 chatJsonSync 触发 JSON 模式（DeepSeek 支持），保证可解析
        return llmService.chatJsonSync(prompt, getSystemPrompt(), getTemperature());
    }

    @Override
    protected String buildPrompt(AgentContext context) {
        Map<String, Object> session = context.getSessionData();
        String conversationText = String.valueOf(session.getOrDefault("conversationText", ""));

        // 现有画像：供模型参考，避免把已知信息抹成空，但不强制沿用
        Map<String, Object> existing = context.getStudentProfile();

        StringBuilder sb = new StringBuilder();
        sb.append("## 任务\n");
        sb.append("阅读下面学生与导师的对话，抽取学生的学习画像特征。\n\n");

        if (existing != null && !existing.isEmpty()) {
            sb.append("## 当前已知画像（仅供参考，对话中有更明确信息时以对话为准）\n");
            existing.forEach((k, v) -> {
                if (v != null && !String.valueOf(v).isBlank()) {
                    sb.append("- ").append(k).append("：").append(v).append("\n");
                }
            });
            sb.append("\n");
        }

        sb.append("## 对话内容\n");
        sb.append(conversationText.isBlank() ? "（无）" : conversationText).append("\n\n");

        sb.append("## 输出格式（严格 JSON，所有字段都要出现）\n");
        sb.append("{\n");
        sb.append("  \"majorDirection\": \"专业方向，如 计算机类 / 电子信息 / 数学，无法判断留空\",\n");
        sb.append("  \"educationLevel\": \"学历层次：专科 / 本科 / 硕士 / 博士，无法判断留空\",\n");
        sb.append("  \"currentCourse\": \"近期最重点学习/最想优先提升的课程名；可多门时只填最优先的一门，无法判断留空\",\n");
        sb.append("  \"learningGoal\": \"学习目标，如 项目实践 / 考研 / 考试通过 / 竞赛，无法判断留空\",\n");
        sb.append("  \"foundationLevel\": \"基础水平：基础 / 中等 / 较强，无法判断留空\",\n");
        sb.append("  \"academicInterest\": \"学术兴趣方向（自由文本，简短），无法判断留空\",\n");
        sb.append("  \"learningStyle\": \"认知风格，只能从以下四选一：visual(图表/动画)、logical(逻辑推导)、example(例题案例)、formula(公式速记)；无法判断留空\",\n");
        sb.append("  \"resourcePreference\": [\"偏好的资源形态，如 教学视频/思维导图/讲解文档/实操案例/分层练习，最多3个\"],\n");
        sb.append("  \"weakModulePriority\": [\"学生自述薄弱或想优先攻克的课程/知识点，格式尽量为 课程名 · 知识点，最多3个\"],\n");
        sb.append("  \"confidence\": 0.0\n");
        sb.append("}\n\n");

        sb.append("## 硬性要求\n");
        sb.append("1. 只输出上面的 JSON，不要任何额外文字。\n");
        sb.append("2. 任何在对话中找不到明确依据的字段，字符串返回 \"\"，数组返回 []，禁止猜测或编造。\n");
        sb.append("3. currentCourse 只表示近期重点课程，不要把专业方向、兴趣方向或系统默认课程当作当前课程。\n");
        sb.append("4. weakModulePriority 要尽量保留科目边界，例如“Java Web 开发 · Servlet 生命周期”；无法判断课程时只写知识点。\n");
        sb.append("5. learningStyle 必须是 visual/logical/example/formula 之一或空串，不得返回其它词。\n");
        sb.append("6. confidence 为 0~1 的小数，表示本次抽取整体的可信度（信息越充分越高）。\n");

        return sb.toString();
    }

    @Override
    protected Map<String, Object> parseResponse(String llmResponse, AgentContext context) {
        Map<String, Object> json = safeParseJson(llmResponse);
        // 归一化：保证关键键存在，类型规整
        Map<String, Object> result = new LinkedHashMap<>(json);
        return result;
    }

    @Override
    protected AgentResponse qualityCheck(Map<String, Object> parsed, AgentContext context) {
        if (parsed == null || parsed.isEmpty()) {
            return AgentResponse.failure("画像抽取失败：未解析出有效 JSON");
        }

        // 统计抽到了多少个有意义的字段
        int meaningful = 0;
        for (Map.Entry<String, Object> e : parsed.entrySet()) {
            if ("confidence".equals(e.getKey())) continue;
            Object v = e.getValue();
            if (v instanceof String && !((String) v).isBlank()) meaningful++;
            else if (v instanceof java.util.List && !((java.util.List<?>) v).isEmpty()) meaningful++;
        }

        double confidence = 0.0;
        Object conf = parsed.get("confidence");
        if (conf instanceof Number) confidence = ((Number) conf).doubleValue();

        if (meaningful == 0) {
            return AgentResponse.failure("画像抽取无有效特征（对话信息不足）");
        }

        Map<String, Object> data = new LinkedHashMap<>(parsed);
        data.put("meaningfulFieldCount", meaningful);
        return AgentResponse.success(
                "成功抽取 " + meaningful + " 个画像特征，置信度 " + String.format("%.2f", confidence),
                data);
    }
}
