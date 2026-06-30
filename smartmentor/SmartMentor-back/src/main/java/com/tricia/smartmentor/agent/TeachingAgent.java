package com.tricia.smartmentor.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tricia.smartmentor.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 教学 Agent —— 根据知识点、学生画像和掌握度生成分层教学内容。
 * <p>
 * 三级内容策略：
 * <ul>
 *   <li>掌握度 &lt; 0.4：基础重教（直观举例 + 图示描述 + 基础练习）</li>
 *   <li>0.4 ~ 0.7：针对性讲解（针对错因 + 变式训练）</li>
 *   <li>&gt; 0.7：进阶应用（综合任务 + 实践案例）</li>
 * </ul>
 * <p>
 * 需要在 {@code context.sessionData} 中预置：
 * <ul>
 *   <li>{@code knowledgePointId} — 当前知识点 ID</li>
 *   <li>{@code knowledgePointName} — 知识点名称</li>
 *   <li>{@code knowledgePointDescription} — 知识点描述（可选）</li>
 *   <li>{@code commonErrors} — 常见错误列表（可选）</li>
 *   <li>{@code masteryLevel} — 当前掌握度 0~1</li>
 * </ul>
 */
@Slf4j
@Component
public class TeachingAgent extends BaseAgent {

    public TeachingAgent(LlmService llmService, ObjectMapper objectMapper) {
        super(llmService, objectMapper);
    }

    @Override
    protected String getName() {
        return "教学Agent";
    }

    @Override
    protected double getTemperature() {
        return 0.7;
    }

    @Override
    protected String getSystemPrompt() {
        String fallback = "你是SmartMentor智学导师系统的资源生成Agent，专门负责高校多专业课程的个性化学习资源生成。" +
                "你的教学风格亲切、清晰，善于用课程案例、图解说明、代码或实践任务帮助学生理解抽象概念。" +
                "你需要根据学生的专业方向、学历层次、学习目标、资源偏好和掌握程度提供不同层次的教学内容。" +
                "请严格按照要求的JSON格式输出结果，不要附加任何额外文字。";
        return loadPromptTemplate(getPromptTemplateKey(), "teaching-system-v1", fallback).getContent();
    }

    @Override
    protected String getPromptTemplateKey() {
        return "teaching-system";
    }

    /** 内容范围：explain=讲解+例题+常见错误；exercise=仅分层练习；full=全部（兼容旧调用）。 */
    public static final String SCOPE_EXPLAIN = "explain";
    public static final String SCOPE_EXERCISE = "exercise";
    public static final String SCOPE_FULL = "full";

    @Override
    @SuppressWarnings("unchecked")
    protected String buildPrompt(AgentContext context) {
        Map<String, Object> sessionData = context.getSessionData();
        Map<String, Object> profile = context.getStudentProfile();

        String scope = String.valueOf(sessionData.getOrDefault("contentScope", SCOPE_FULL));
        String kpId = (String) sessionData.getOrDefault("knowledgePointId", "");
        String kpName = (String) sessionData.getOrDefault("knowledgePointName", "未知知识点");
        String kpDesc = (String) sessionData.getOrDefault("knowledgePointDescription", "");
        double masteryLevel = 0.5;
        Object mlObj = sessionData.get("masteryLevel");
        if (mlObj instanceof Number) {
            masteryLevel = ((Number) mlObj).doubleValue();
        }

        List<String> commonErrors = Collections.emptyList();
        Object ceObj = sessionData.get("commonErrors");
        if (ceObj instanceof List) {
            commonErrors = (List<String>) ceObj;
        }

        // 学生错误模式
        Map<String, Integer> errorPatterns = context.getErrorPatterns();

        // 确定教学策略
        String strategy;
        String strategyDesc;
        int exerciseCount;
        if (masteryLevel < 0.4) {
            strategy = "foundation";
            strategyDesc = "基础重教策略：使用直观生活实例引入概念，配合图示描述帮助建立直觉，练习以基础题为主";
            exerciseCount = 3;
        } else if (masteryLevel < 0.7) {
            strategy = "targeted";
            strategyDesc = "针对性讲解策略：聚焦学生常犯错误，通过变式训练强化薄弱环节，练习难度中等";
            exerciseCount = 4;
        } else {
            strategy = "advanced";
            strategyDesc = "进阶应用策略：以综合任务和实践案例为主，训练迁移应用和综合分析能力";
            exerciseCount = 5;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## 任务\n");
        if (SCOPE_EXERCISE.equals(scope)) {
            sb.append("请为以下知识点生成一组高质量的分层练习题（仅练习题，不要讲解和例题）。\n\n");
        } else if (SCOPE_EXPLAIN.equals(scope)) {
            sb.append("请为以下知识点生成成体系的讲解内容（概念精讲 + 典型例题 + 常见错误，不要练习题）。\n\n");
        } else {
            sb.append("请为以下知识点生成完整的教学内容。\n\n");
        }

        sb.append("## 知识点信息\n");
        sb.append("- ID：").append(kpId).append("\n");
        sb.append("- 名称：").append(kpName).append("\n");
        if (!kpDesc.isEmpty()) {
            sb.append("- 描述：").append(kpDesc).append("\n");
        }
        sb.append("- 所属模块：").append(context.getModule()).append("\n\n");

        sb.append("## 学生情况\n");
        sb.append("- 当前掌握度：").append(String.format("%.2f", masteryLevel)).append("\n");
        sb.append("- 教学策略：").append(strategyDesc).append("\n");
        if (profile != null && !profile.isEmpty()) {
            appendProfileLine(sb, profile, "majorDirection", "专业方向");
            appendProfileLine(sb, profile, "currentCourse", "当前课程");
            appendProfileLine(sb, profile, "educationLevel", "学历层次");
            appendProfileLine(sb, profile, "learningGoal", "学习目标");
            appendProfileLine(sb, profile, "foundationLevel", "基础水平");
            appendProfileLine(sb, profile, "weakModulePriority", "薄弱模块");
            appendProfileLine(sb, profile, "academicInterest", "兴趣方向");
            appendProfileLine(sb, profile, "learningStyle", "学习风格");
            // 兼容旧字段命名
            appendProfileLine(sb, profile, "grade", "学历层次");
        }
        Object learningTarget = sessionData.get("learningTarget");
        if (learningTarget != null && !String.valueOf(learningTarget).isBlank()) {
            sb.append("- 学习路径目标：").append(learningTarget).append("\n");
        }
        Object rootCause = sessionData.get("rootCausePoint");
        if (rootCause != null && !String.valueOf(rootCause).isBlank()) {
            sb.append("- 根因薄弱点：").append(rootCause).append("\n");
        }
        sb.append("\n");

        // 画像驱动：讲解/完整内容按学习风格定制呈现方式（纯练习题 scope 不需要）
        if (!SCOPE_EXERCISE.equals(scope) && profile != null) {
            appendLearningStyleGuidance(sb, String.valueOf(profile.getOrDefault("learningStyle", "")));
        }

        if (!commonErrors.isEmpty()) {
            sb.append("## 该知识点常见错误\n");
            for (String error : commonErrors) {
                sb.append("- ").append(error).append("\n");
            }
            sb.append("\n");
        }

        if (errorPatterns != null && !errorPatterns.isEmpty()) {
            sb.append("## 该学生的错误模式（历史统计）\n");
            errorPatterns.forEach((pattern, count) ->
                    sb.append("- ").append(pattern).append("：").append(count).append("次\n"));
            sb.append("\n");
        }

        if (SCOPE_EXERCISE.equals(scope)) {
            appendExercisePrompt(sb, exerciseCount);
        } else if (SCOPE_EXPLAIN.equals(scope)) {
            appendExplainPrompt(sb, strategy);
        } else {
            appendFullPrompt(sb, strategy, exerciseCount);
        }

        return sb.toString();
    }

    /**
     * 画像驱动：按学习风格给出讲解呈现方式的明确指令，让同一知识点对不同风格的学生有差异化讲法。
     * 枚举：visual(视觉) / logical(逻辑) / example(案例) / formula(公式)。
     */
    private void appendLearningStyleGuidance(StringBuilder sb, String style) {
        if (style == null || style.isBlank() || "null".equals(style)) {
            return;
        }
        sb.append("## 讲解呈现方式（按该生学习风格定制，务必体现）\n");
        switch (style) {
            case "visual":
                sb.append("该生为【视觉型】：多用结构化对比、分点列举、图示化描述；")
                  .append("在 visualDescription 中给出可画成流程图/结构图的清晰说明，关键关系用表格或层级呈现，少用大段纯文字。\n\n");
                break;
            case "logical":
                sb.append("该生为【逻辑型】：按『定义→原理→推导→结论』的逻辑链条讲解，")
                  .append("讲清每一步的前因后果与推理依据，强调概念之间的逻辑关系，避免跳步。\n\n");
                break;
            case "example":
                sb.append("该生为【案例型】：先给具体典型例题/真实场景，再从例子归纳抽象概念；")
                  .append("例题要分步详解，并配一两个变式，让学生从『做中学』。\n\n");
                break;
            case "formula":
                sb.append("该生为【公式型】：突出核心公式与定理，给出公式的速记口诀/记忆技巧和适用条件，")
                  .append("提供『套用模板』式的解题步骤，便于按公式快速求解。\n\n");
                break;
            default:
                // 未知风格不加约束
                break;
        }
    }

    /** 讲解块：概念精讲 + 例题 + 常见错误 + 小结（不含练习题）。 */
    private void appendExplainPrompt(StringBuilder sb, String strategy) {
        sb.append("## 输出要求\n");
        sb.append("请以JSON格式返回，结构如下（只输出JSON本身，不要用markdown代码块包裹）：\n");
        sb.append("{\n");
        sb.append("  \"title\": \"教学标题\",\n");
        sb.append("  \"strategy\": \"").append(strategy).append("\",\n");
        sb.append("  \"conceptExplanation\": {\n");
        sb.append("    \"summary\": \"一句话概括本知识点核心内容\",\n");
        sb.append("    \"content\": \"成体系的概念讲解（Markdown格式，按 是什么/为什么/适用条件/怎么用/易错点 分段展开，不少于400字）\",\n");
        sb.append("    \"keyFormulas\": [\"关键公式1（LaTeX格式）\", \"关键公式2\"],\n");
        sb.append("    \"visualDescription\": \"帮助理解的图示或直观描述\"\n");
        sb.append("  },\n");
        sb.append("  \"examples\": [\n");
        sb.append("    {\n");
        sb.append("      \"title\": \"例题标题\",\n");
        sb.append("      \"problem\": \"题目内容\",\n");
        sb.append("      \"solution\": \"分步骤详细解答过程，每步说明在做什么、为什么这样做\",\n");
        sb.append("      \"keyPoint\": \"本题考察的关键点\"\n");
        sb.append("    }\n");
        sb.append("  ],\n");
        sb.append("  \"commonMistakes\": [\n");
        sb.append("    {\"mistake\": \"常见错误描述\", \"correction\": \"正确做法和避免建议\"}\n");
        sb.append("  ],\n");
        sb.append("  \"summary\": \"本节要点总结\"\n");
        sb.append("}\n\n");
        sb.append("要求：\n");
        sb.append("1. conceptExplanation.content 必须分点/分段展开、内容详实，不少于400字，禁止用没有信息量的套话敷衍\n");
        sb.append("2. examples 至少包含2道例题，每道都有完整的分步解答过程\n");
        sb.append("3. 公式必须使用LaTeX语法并用 $...$ 或 $$...$$ 包裹，禁止裸LaTeX\n");
        sb.append("4. 内容要匹配学生学历层次和课程基础，讲解清晰易懂、由浅入深\n");
        sb.append("5. commonMistakes 至少列出2个常见错误及纠正方法\n");
        sb.append("6. 不要用字符画/ASCII Art绘制图表、流程图或结构图；如需说明结构，用关键点列表、关系说明和操作步骤替代\n");
    }

    /** 练习块：仅分层练习题。 */
    private void appendExercisePrompt(StringBuilder sb, int exerciseCount) {
        sb.append("## 输出要求\n");
        sb.append("请以JSON格式返回，结构如下（只输出JSON本身，不要用markdown代码块包裹）：\n");
        sb.append("{\n");
        sb.append("  \"exercises\": [\n");
        sb.append("    {\n");
        sb.append("      \"id\": \"ex1\",\n");
        sb.append("      \"problem\": \"练习题目\",\n");
        sb.append("      \"options\": {\"A\": \"选项A\", \"B\": \"选项B\", \"C\": \"选项C\", \"D\": \"选项D\"},\n");
        sb.append("      \"correctAnswer\": \"B\",\n");
        sb.append("      \"explanation\": \"解析，说明为什么选这个、其他选项错在哪\",\n");
        sb.append("      \"difficulty\": 2\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n\n");
        sb.append("要求：\n");
        sb.append("1. exercises 包含").append(exerciseCount).append("道练习题，难度由易到难递进\n");
        sb.append("2. 每题都是单选题，options 必须有 A/B/C/D 四个选项，correctAnswer 为其中一个键\n");
        sb.append("3. explanation 要讲清正确答案的依据，并指出典型错误选项的成因\n");
        sb.append("4. 公式必须使用LaTeX语法并用 $...$ 或 $$...$$ 包裹，禁止裸LaTeX\n");
        sb.append("5. 题目要紧扣该知识点，覆盖不同考察角度，避免雷同\n");
    }

    /** 全量块（兼容旧调用方）。 */
    private void appendFullPrompt(StringBuilder sb, String strategy, int exerciseCount) {
        sb.append("## 输出要求\n");
        sb.append("请以JSON格式返回，结构如下：\n");
        sb.append("```json\n");
        sb.append("{\n");
        sb.append("  \"title\": \"教学标题\",\n");
        sb.append("  \"strategy\": \"").append(strategy).append("\",\n");
        sb.append("  \"conceptExplanation\": {\n");
        sb.append("    \"summary\": \"一句话概括本知识点核心内容\",\n");
        sb.append("    \"content\": \"详细的概念讲解内容（使用Markdown格式，可包含LaTeX公式如$f'(x)$）\",\n");
        sb.append("    \"keyFormulas\": [\"关键公式1（LaTeX格式）\", \"关键公式2\"],\n");
        sb.append("    \"visualDescription\": \"帮助理解的图示或直观描述\"\n");
        sb.append("  },\n");
        sb.append("  \"examples\": [\n");
        sb.append("    {\n");
        sb.append("      \"title\": \"例题标题\",\n");
        sb.append("      \"problem\": \"题目内容\",\n");
        sb.append("      \"solution\": \"分步骤详细解答过程\",\n");
        sb.append("      \"keyPoint\": \"本题考察的关键点\"\n");
        sb.append("    }\n");
        sb.append("  ],\n");
        sb.append("  \"exercises\": [\n");
        sb.append("    {\n");
        sb.append("      \"id\": \"ex1\",\n");
        sb.append("      \"problem\": \"练习题目\",\n");
        sb.append("      \"options\": {\"A\": \"选项A\", \"B\": \"选项B\", \"C\": \"选项C\", \"D\": \"选项D\"},\n");
        sb.append("      \"correctAnswer\": \"B\",\n");
        sb.append("      \"explanation\": \"解析\",\n");
        sb.append("      \"difficulty\": 2\n");
        sb.append("    }\n");
        sb.append("  ],\n");
        sb.append("  \"commonMistakes\": [\n");
        sb.append("    {\n");
        sb.append("      \"mistake\": \"常见错误描述\",\n");
        sb.append("      \"correction\": \"正确做法和避免建议\"\n");
        sb.append("    }\n");
        sb.append("  ],\n");
        sb.append("  \"summary\": \"本节要点总结（简明扼要）\"\n");
        sb.append("}\n");
        sb.append("```\n\n");
        sb.append("要求：\n");
        sb.append("1. examples 至少包含2道例题，有详细的分步解答\n");
        sb.append("2. exercises 包含").append(exerciseCount).append("道练习题，难度递进\n");
        sb.append("3. 公式必须使用LaTeX语法并用 $...$ 或 $$...$$ 包裹，禁止裸LaTeX\n");
        sb.append("4. 内容要匹配学生学历层次和课程基础，讲解清晰易懂\n");
        sb.append("5. commonMistakes 至少列出2个常见错误及纠正方法\n");
        sb.append("6. 不要用字符画/ASCII Art绘制图表、流程图或结构图；如需说明结构，用关键点表、关系说明和操作步骤替代\n");
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, Object> parseResponse(String llmResponse, AgentContext context) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> json = safeParseJson(llmResponse);

            result.put("title", json.getOrDefault("title", "教学内容"));
            result.put("strategy", json.getOrDefault("strategy", "targeted"));
            result.put("conceptExplanation", json.getOrDefault("conceptExplanation", new HashMap<>()));
            result.put("examples", json.getOrDefault("examples", Collections.emptyList()));
            result.put("exercises", json.getOrDefault("exercises", Collections.emptyList()));
            result.put("commonMistakes", json.getOrDefault("commonMistakes", Collections.emptyList()));
            result.put("summary", json.getOrDefault("summary", ""));

            // 诊断：讲解块解析后 concept 为空时，记录原始返回，区分“解析失败”还是“模型未按格式返回”
            String scope = String.valueOf(context.getSessionData().getOrDefault("contentScope", SCOPE_FULL));
            if (SCOPE_EXPLAIN.equals(scope)) {
                Object concept = result.get("conceptExplanation");
                boolean conceptEmpty = !(concept instanceof Map)
                        || String.valueOf(((Map<?, ?>) concept).get("content")).isBlank()
                        || "null".equals(String.valueOf(((Map<?, ?>) concept).get("content")));
                if (conceptEmpty) {
                    int len = llmResponse == null ? 0 : llmResponse.length();
                    String head = llmResponse == null ? "<null>"
                            : llmResponse.substring(0, Math.min(600, llmResponse.length()));
                    log.warn("[{}] 讲解块concept为空 | jsonParsedKeys={} | rawLen={} | rawHead={}",
                            getName(), json.keySet(), len, head);
                }
            }

        } catch (Exception e) {
            log.error("[{}] 解析教学内容失败: {}", getName(), e.getMessage());
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected AgentResponse qualityCheck(Map<String, Object> parsed, AgentContext context) {
        String scope = String.valueOf(context.getSessionData().getOrDefault("contentScope", SCOPE_FULL));

        Object examplesObj = parsed.get("examples");
        Object exercisesObj = parsed.get("exercises");
        Object conceptObj = parsed.get("conceptExplanation");

        boolean hasExamples = examplesObj instanceof List && !((List<?>) examplesObj).isEmpty();
        boolean hasExercises = exercisesObj instanceof List && !((List<?>) exercisesObj).isEmpty();
        boolean hasConcept = conceptObj instanceof Map
                && !String.valueOf(((Map<?, ?>) conceptObj).get("content")).isBlank();

        int exampleCount = hasExamples ? ((List<?>) examplesObj).size() : 0;
        int exerciseCount = hasExercises ? ((List<?>) exercisesObj).size() : 0;

        if (SCOPE_EXERCISE.equals(scope)) {
            if (!hasExercises) {
                return AgentResponse.failure("练习内容质量不合格：缺少练习题");
            }
            return AgentResponse.success("练习生成完成：" + exerciseCount + "道练习题",
                    parsed, AgentEvent.LESSON_GENERATED);
        }

        if (SCOPE_EXPLAIN.equals(scope)) {
            if (!hasConcept && !hasExamples) {
                return AgentResponse.failure("讲解内容质量不合格：缺少概念讲解和例题");
            }
            return AgentResponse.success(
                    String.format("讲解生成完成：概念讲解 + %d道例题", exampleCount),
                    parsed, AgentEvent.LESSON_GENERATED);
        }

        if (!hasExamples && !hasExercises) {
            return AgentResponse.failure("教学内容质量不合格：缺少例题和练习题");
        }

        String message = String.format("教学内容生成完成：%d道例题 + %d道练习题",
                exampleCount, exerciseCount);

        return AgentResponse.success(message, parsed, AgentEvent.LESSON_GENERATED);
    }
}
