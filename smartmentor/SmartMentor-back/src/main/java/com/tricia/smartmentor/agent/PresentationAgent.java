package com.tricia.smartmentor.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tricia.smartmentor.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 演示文稿（PPT）生成 Agent。
 * <p>
 * 与 {@link TeachingAgent}（讲解/练习）、{@link ResourceAgent}（思维导图/拓展阅读/实操案例/动画脚本）
 * 分工协作，专门把某知识点的已生成学习素材，按学生画像重组为一份结构化的演示文稿大纲（slides JSON）。
 * 产物用于前端 reveal.js 在线演示与后端 Apache POI 导出 .pptx，做到「点击即出、千人千面」。
 * <p>
 * 需要在 {@code context.sessionData} 中预置：
 * <ul>
 *   <li>{@code knowledgePointName} — 知识点名称</li>
 *   <li>{@code knowledgePointDescription} — 知识点描述（可选）</li>
 *   <li>{@code masteryLevel} — 当前掌握度 0~1，越低页数越多、步子越小</li>
 *   <li>{@code lessonMaterial} — 由 LearningService 汇总的讲解/例题/小结/思维导图 Markdown 素材</li>
 * </ul>
 * 生成失败或质量不足时，由调用方（LearningService）回退到基于课程素材的模板大纲兜底，保证可用性。
 */
@Slf4j
@Component
public class PresentationAgent extends BaseAgent {

    /** 合法的幻灯片类型（前端 reveal.js 与后端 POI 都按这些类型渲染）。 */
    public static final List<String> SLIDE_TYPES = List.of(
            "cover", "agenda", "content", "code", "formula", "case", "summary");

    public PresentationAgent(LlmService llmService, ObjectMapper objectMapper) {
        super(llmService, objectMapper);
    }

    @Override
    protected String getName() {
        return "演示文稿Agent";
    }

    @Override
    protected double getTemperature() {
        return 0.6;
    }

    @Override
    protected String getSystemPrompt() {
        return "你是SmartMentor智学导师系统的演示文稿生成Agent，擅长把一节课的学习内容重组为逻辑清晰、"
                + "适合课堂演示的幻灯片。你会依据学生的专业方向、学历层次与当前掌握度调整讲解深度、页数与示例领域："
                + "掌握度低则多铺垫、少术语、步子小、页数偏多；掌握度高则精炼、重综合应用。"
                + "幻灯片要点精炼，每页聚焦一个主题，避免整段文字堆砌。"
                + "请严格按要求的JSON格式输出，只输出JSON本身，不要用markdown代码块包裹。";
    }

    @Override
    protected String buildPrompt(AgentContext context) {
        Map<String, Object> session = context.getSessionData();
        Map<String, Object> profile = context.getStudentProfile();

        String kpName = String.valueOf(session.getOrDefault("knowledgePointName", "未知知识点"));
        String kpDesc = String.valueOf(session.getOrDefault("knowledgePointDescription", ""));
        String material = String.valueOf(session.getOrDefault("lessonMaterial", ""));
        double mastery = 0.5;
        Object ml = session.get("masteryLevel");
        if (ml instanceof Number) {
            mastery = ((Number) ml).doubleValue();
        }
        int minPages = mastery < 0.4 ? 8 : (mastery < 0.7 ? 7 : 6);
        int maxPages = mastery < 0.4 ? 12 : (mastery < 0.7 ? 10 : 9);

        StringBuilder sb = new StringBuilder();
        sb.append("## 任务\n");
        sb.append("基于下面的课程素材，为这位学生制作一份围绕「").append(kpName)
                .append("」的个性化演示文稿（PPT）大纲。\n\n");

        sb.append("## 知识点信息\n");
        sb.append("- 名称：").append(kpName).append("\n");
        if (kpDesc != null && !kpDesc.isBlank() && !"null".equals(kpDesc)) {
            sb.append("- 描述：").append(kpDesc).append("\n");
        }
        sb.append("- 所属模块/课程：")
                .append(context.getModule() != null ? context.getModule() : "高校课程").append("\n");
        sb.append("- 学生当前掌握度：").append(String.format("%.2f", mastery))
                .append("（0~1，越低越要基础友好、铺垫充分）\n\n");

        sb.append("## 学生画像（演示文稿须据此个性化）\n");
        appendProfileLine(sb, profile, "majorDirection", "专业方向");
        appendProfileLine(sb, profile, "currentCourse", "当前课程");
        appendProfileLine(sb, profile, "educationLevel", "学历层次");
        appendProfileLine(sb, profile, "learningGoal", "学习目标");
        appendProfileLine(sb, profile, "foundationLevel", "基础水平");
        appendProfileLine(sb, profile, "learningStyle", "学习风格");
        appendProfileLine(sb, profile, "academicInterest", "兴趣方向");
        sb.append("\n");

        if (material != null && !material.isBlank()) {
            sb.append("## 课程素材（请据此提炼，不要脱离素材凭空发挥）\n");
            sb.append(material.length() > 4000 ? material.substring(0, 4000) : material).append("\n\n");
        }

        sb.append("## 输出格式（严格JSON）\n");
        sb.append("{\n");
        sb.append("  \"meta\": {\n");
        sb.append("    \"title\": \"演示文稿主标题\",\n");
        sb.append("    \"subtitle\": \"副标题（一句话点明本节定位）\",\n");
        sb.append("    \"theme\": \"tech-blue\",\n");
        sb.append("    \"audience\": \"目标受众（如：本科·计算机类·基础阶段）\"\n");
        sb.append("  },\n");
        sb.append("  \"slides\": [\n");
        sb.append("    {\"type\": \"cover\", \"title\": \"封面标题\", \"subtitle\": \"封面副标题\"},\n");
        sb.append("    {\"type\": \"agenda\", \"title\": \"本节脉络\", \"points\": [\"脉络1\", \"脉络2\", \"脉络3\"]},\n");
        sb.append("    {\"type\": \"content\", \"title\": \"内容页标题\", \"bullets\": [\"要点1\", \"要点2\"], \"note\": \"讲者备注（可选）\"},\n");
        sb.append("    {\"type\": \"code\", \"title\": \"代码示例标题\", \"lang\": \"java\", \"code\": \"示例代码\", \"explain\": \"代码讲解\"},\n");
        sb.append("    {\"type\": \"formula\", \"title\": \"公式页标题\", \"latex\": \"LaTeX公式（不含$符号）\", \"explain\": \"公式含义说明\"},\n");
        sb.append("    {\"type\": \"case\", \"title\": \"实操案例标题\", \"scenario\": \"结合专业的应用场景\", \"steps\": [\"步骤1\", \"步骤2\", \"步骤3\"]},\n");
        sb.append("    {\"type\": \"summary\", \"title\": \"小结\", \"points\": [\"要点回顾1\", \"要点回顾2\"]}\n");
        sb.append("  ]\n");
        sb.append("}\n\n");

        sb.append("要求：\n");
        sb.append("1. slides 总页数控制在 ").append(minPages).append("~").append(maxPages)
                .append(" 页，首页必须是 cover，末页必须是 summary，第二页建议是 agenda\n");
        sb.append("2. 每页只用上面列出的 type，字段按对应 type 填写；content 的 bullets 每条精炼（不超过30字），每页 2~5 条\n");
        sb.append("3. 仅当该知识点确实涉及编程时才用 code 页且 code 必须是可读示例；确实涉及公式时才用 formula 页\n");
        sb.append("4. case 页要结合学生专业方向给出真实应用场景，steps 至少3步\n");
        sb.append("5. 内容紧扣课程素材与「").append(kpName).append("」，匹配学生学历与掌握度，禁止套话与无关泛泛而谈\n");
        sb.append("6. 禁止用字符画/ASCII Art 绘制图表；formula 的 latex 字段不要带 $ 包裹符号\n");

        return sb.toString();
    }

    @Override
    protected Map<String, Object> parseResponse(String llmResponse, AgentContext context) {
        return safeParseJson(llmResponse);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected AgentResponse qualityCheck(Map<String, Object> parsed, AgentContext context) {
        if (parsed == null || parsed.isEmpty()) {
            return AgentResponse.failure("演示文稿生成失败：未解析出有效内容");
        }
        Object slidesObj = parsed.get("slides");
        if (!(slidesObj instanceof List) || ((List<?>) slidesObj).size() < 4) {
            return AgentResponse.failure("演示文稿质量不足：有效幻灯片不足4页");
        }
        long valid = ((List<Object>) slidesObj).stream()
                .filter(s -> s instanceof Map && SLIDE_TYPES.contains(String.valueOf(((Map<?, ?>) s).get("type"))))
                .count();
        if (valid < 4) {
            return AgentResponse.failure("演示文稿质量不足：合法类型幻灯片仅 " + valid + " 页");
        }
        return AgentResponse.success("演示文稿生成完成，共 " + ((List<?>) slidesObj).size() + " 页",
                parsed, AgentEvent.RESOURCE_GENERATED);
    }
}
