package com.tricia.smartmentor.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tricia.smartmentor.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 多模态学习资源生成 Agent。
 * <p>
 * 与 {@link TeachingAgent}（负责讲解与练习）分工协作，专门负责生成「非讲解类」的
 * 多模态个性化学习资源，一次产出四类内容：
 * <ul>
 *   <li>mindMap —— 知识点思维导图（多级结构）</li>
 *   <li>extendedReading —— 拓展阅读材料（贴合学生专业方向）</li>
 *   <li>practiceCase —— 实操案例（结合专业应用场景，可含代码类实操）</li>
 *   <li>animationScript —— 多模态动画/视频讲解脚本（分镜）</li>
 * </ul>
 * 所有内容均基于学生画像（专业、课程、学历、掌握度、薄弱点）个性化生成，
 * 而非固定模板拼接。生成失败时由调用方（LearningService）回退到模板兜底，保证可用性。
 */
@Slf4j
@Component
public class ResourceAgent extends BaseAgent {

    /** 本 Agent 负责生成的资源类型键。 */
    public static final List<String> RESOURCE_KEYS =
            List.of("mindMap", "extendedReading", "practiceCase", "animationScript");

    public ResourceAgent(LlmService llmService, ObjectMapper objectMapper) {
        super(llmService, objectMapper);
    }

    @Override
    protected String getName() {
        return "资源生成Agent";
    }

    @Override
    protected double getTemperature() {
        return 0.7;
    }

    @Override
    protected String getSystemPrompt() {
        return "你是SmartMentor智学导师系统的资源生成Agent，专门为高校学生生成多模态个性化学习资源。"
                + "你会依据学生的专业方向、所学课程、学历层次、当前掌握度与薄弱点，"
                + "围绕指定知识点生成思维导图、拓展阅读、实操案例、动画脚本四类资源。"
                + "这四类资源会同时呈现给同一个学生，因此必须各司其职、内容互不重叠："
                + "思维导图给知识结构骨架、拓展阅读给外部资料索引、实操案例给可动手的具体任务、动画讲解给过程可视化。"
                + "资源必须紧扣学生专业与真实应用场景，内容具体、有信息量，禁止套话与无关泛泛而谈，禁止四类资源讲同一件事。"
                + "请严格按要求的JSON格式输出，只输出JSON本身，不要用markdown代码块包裹。";
    }

    @Override
    protected String buildPrompt(AgentContext context) {
        Map<String, Object> session = context.getSessionData();
        Map<String, Object> profile = context.getStudentProfile();

        String kpName = String.valueOf(session.getOrDefault("knowledgePointName", "未知知识点"));
        String kpDesc = String.valueOf(session.getOrDefault("knowledgePointDescription", ""));
        String moduleName = context.getModule() != null ? context.getModule() : "高校课程";
        double mastery = 0.5;
        Object ml = session.get("masteryLevel");
        if (ml instanceof Number) {
            mastery = ((Number) ml).doubleValue();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## 任务\n");
        sb.append("围绕下面的知识点，为这位学生生成四类多模态学习资源。\n\n");

        sb.append("## 知识点信息\n");
        sb.append("- 名称：").append(kpName).append("\n");
        if (kpDesc != null && !kpDesc.isBlank() && !"null".equals(kpDesc)) {
            sb.append("- 描述：").append(kpDesc).append("\n");
        }
        sb.append("- 所属模块/课程：").append(moduleName).append("\n");
        sb.append("- 学生当前掌握度：").append(String.format("%.2f", mastery)).append("（0~1，越低越要基础友好）\n\n");

        sb.append("## 学生画像（资源须据此个性化）\n");
        appendProfileLine(sb, profile, "majorDirection", "专业方向");
        appendProfileLine(sb, profile, "currentCourse", "当前课程");
        appendProfileLine(sb, profile, "educationLevel", "学历层次");
        appendProfileLine(sb, profile, "learningGoal", "学习目标");
        appendProfileLine(sb, profile, "foundationLevel", "基础水平");
        appendProfileLine(sb, profile, "weakModulePriority", "薄弱模块");
        appendProfileLine(sb, profile, "academicInterest", "兴趣方向");
        appendProfileLine(sb, profile, "resourcePreference", "资源偏好");
        Object learningTarget = session.get("learningTarget");
        if (learningTarget != null && !String.valueOf(learningTarget).isBlank()) {
            sb.append("- 学习路径目标：").append(learningTarget).append("\n");
        }
        Object rootCause = session.get("rootCausePoint");
        if (rootCause != null && !String.valueOf(rootCause).isBlank()) {
            sb.append("- 根因薄弱点：").append(rootCause).append("\n");
        }
        sb.append("\n");

        sb.append("## 输出格式（严格JSON）\n");
        sb.append("{\n");
        sb.append("  \"mindMap\": {\n");
        sb.append("    \"title\": \"思维导图标题\",\n");
        sb.append("    \"summary\": \"一句话概括（不超过40字，用作卡片摘要）\",\n");
        sb.append("    \"root\": \"中心主题\",\n");
        sb.append("    \"branches\": [\n");
        sb.append("      {\"topic\": \"分支主题\", \"points\": [\"要点1\", \"要点2\"]}\n");
        sb.append("    ]\n");
        sb.append("  },\n");
        sb.append("  \"extendedReading\": {\n");
        sb.append("    \"title\": \"拓展阅读标题\",\n");
        sb.append("    \"summary\": \"一句话概括（不超过40字）\",\n");
        sb.append("    \"items\": [\n");
        sb.append("      {\"topic\": \"具体资料名/检索关键词\", \"description\": \"资料类型（书籍章节/论文/官方文档/开源项目/公开课）+ 在哪找 + 读这份能补什么\"}\n");
        sb.append("    ]\n");
        sb.append("  },\n");
        sb.append("  \"practiceCase\": {\n");
        sb.append("    \"title\": \"实操案例标题\",\n");
        sb.append("    \"summary\": \"一句话概括（不超过40字）\",\n");
        sb.append("    \"scenario\": \"一个具体的动手任务（有明确的输入与目标产出，不是泛泛的应用方向）\",\n");
        sb.append("    \"steps\": [\"可照做的操作步骤1\", \"步骤2\", \"步骤3\"],\n");
        sb.append("    \"sampleCode\": \"涉及编程则给可运行示例代码；纯理论则给具体演算/推导过程，不要留空\",\n");
        sb.append("    \"expectedResult\": \"预期结果 + 自检要点\"\n");
        sb.append("  },\n");
        sb.append("  \"animationScript\": {\n");
        sb.append("    \"title\": \"动画讲解脚本标题\",\n");
        sb.append("    \"summary\": \"一句话概括（不超过40字）\",\n");
        sb.append("    \"scenes\": [\n");
        sb.append("      {\n");
        sb.append("        \"scene\": \"场景名（如：问题引入）\",\n");
        sb.append("        \"narration\": \"旁白解说\",\n");
        sb.append("        \"visual\": \"画面描述\",\n");
        sb.append("        \"diagram\": {\n");
        sb.append("          \"type\": \"flow\",\n");
        sb.append("          \"nodes\": [{\"id\": \"a\", \"label\": \"节点名（≤6字）\"}, {\"id\": \"b\", \"label\": \"节点名\"}],\n");
        sb.append("          \"edges\": [{\"from\": \"a\", \"to\": \"b\", \"label\": \"边说明（可空，≤6字）\"}]\n");
        sb.append("        }\n");
        sb.append("      }\n");
        sb.append("    ]\n");
        sb.append("  }\n");
        sb.append("}\n\n");

        sb.append("## 四类资源的职责边界（关键：必须各司其职，禁止内容重叠）\n");
        sb.append("这四类资源会同时展示给同一个学生，若内容雷同则毫无价值。请严格区分：\n");
        sb.append("- **mindMap（思维导图）**：只做\"知识结构骨架\"——把该知识点拆成概念层级与分支关系。只列概念节点和要点关键词，不展开讲解、不举应用例子。\n");
        sb.append("- **extendedReading（拓展阅读）**：只做\"指向外部的资料清单\"——给出具体的书籍章节/论文/官方文档/开源项目/公开课名称与检索关键词。是\"去哪儿继续学\"的索引，不是自己讲内容、不写操作步骤。\n");
        sb.append("- **practiceCase（实操案例）**：只做\"一个可动手完成的具体任务\"——有明确输入和目标产出，给出可照做的步骤、示例代码/演算、预期结果与自检。是\"动手做一遍\"，不是泛泛说\"应用到某场景\"。\n");
        sb.append("- **animationScript（动画讲解）**：只做\"分镜化的过程演示\"——用 diagram 流程图把知识点的逻辑流转/运行机制可视化，配旁白。重点是\"动态展示过程怎么走\"，不是罗列知识要点。\n");
        sb.append("自检：如果某两类资源点开后讲的是同一件事，说明你没分清职责，必须重写其中一类换角度。\n\n");

        sb.append("要求：\n");
        sb.append("1. 四类资源都必须紧扣「").append(kpName).append("」且贴合学生专业方向，内容具体有信息量\n");
        sb.append("2. extendedReading 的 items 至少3条（每条是具体可检索的资料，不是泛泛方向）；practiceCase 的 steps 至少3步且可照做\n");
        sb.append("3. mindMap 的 branches 至少3个分支，animationScript 的 scenes 至少3个场景\n");
        sb.append("4. 涉及公式用 $...$ 包裹；涉及编程的知识点 practiceCase.sampleCode 必须给出可读的示例代码\n");
        sb.append("5. 禁止用字符画/ASCII Art 绘制图表\n");
        sb.append("6. animationScript 每个场景必须给出 diagram：把该场景要讲的过程/结构画成流程图，"
                + "nodes 是2~5个关键环节（label≤6字），edges 用 from/to 连接节点ID表示流向/关系（label 标注关系，可空）。"
                + "diagram 要真实反映该知识点的逻辑流转（如：输入→处理→输出、误差反向回传等），不要凑数。\n");

        return sb.toString();
    }

    private void appendProfileLine(StringBuilder sb, Map<String, Object> profile, String key, String label) {
        if (profile == null) return;
        Object v = profile.get(key);
        if (v != null && !String.valueOf(v).isBlank() && !"null".equals(String.valueOf(v))) {
            sb.append("- ").append(label).append("：").append(v).append("\n");
        }
    }

    @Override
    protected Map<String, Object> parseResponse(String llmResponse, AgentContext context) {
        return safeParseJson(llmResponse);
    }

    @Override
    protected AgentResponse qualityCheck(Map<String, Object> parsed, AgentContext context) {
        if (parsed == null || parsed.isEmpty()) {
            return AgentResponse.failure("资源生成失败：未解析出有效内容");
        }
        long ok = RESOURCE_KEYS.stream()
                .filter(k -> parsed.get(k) instanceof Map && !((Map<?, ?>) parsed.get(k)).isEmpty())
                .count();
        // 至少生成 3/4 类才算合格，否则交由调用方模板兜底
        if (ok < 3) {
            return AgentResponse.failure("资源生成质量不足：有效资源类型仅 " + ok + "/4");
        }
        return AgentResponse.success("多模态资源生成完成，有效类型 " + ok + "/4",
                parsed, AgentEvent.RESOURCE_GENERATED);
    }

    /** 空安全获取某类资源的子对象，不存在时返回空 Map。 */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> extractResource(Map<String, Object> data, String key) {
        if (data == null) return Collections.emptyMap();
        Object v = data.get(key);
        return v instanceof Map ? (Map<String, Object>) v : Collections.emptyMap();
    }
}
