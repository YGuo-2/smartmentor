package com.tricia.smartmentor.agent;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tricia.smartmentor.entity.AgentRunLog;
import com.tricia.smartmentor.repository.AgentRunLogRepository;
import com.tricia.smartmentor.service.LlmService;
import com.tricia.smartmentor.service.PromptTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 所有专项 Agent 的抽象基类，实现模板方法模式。
 * <p>
 * 执行流程固定为：
 * <ol>
 *   <li>buildPrompt  — 根据上下文组装提示词</li>
 *   <li>callLLM      — 调用 DeepSeek 大模型（同步）</li>
 *   <li>parseResponse — 将 LLM 原始文本解析为结构化 Map</li>
 *   <li>qualityCheck — 对解析结果进行质量校验并构造 AgentResponse</li>
 * </ol>
 * <p>
 * 子类只需实现 {@link #getName()}、{@link #buildPrompt}、
 * {@link #parseResponse} 和 {@link #qualityCheck} 四个抽象方法，
 * 可选覆盖 {@link #getSystemPrompt()} 和 {@link #getTemperature()} 以调整行为。
 */
@Slf4j
public abstract class BaseAgent {

    protected final LlmService llmService;
    protected final ObjectMapper objectMapper;
    /**
     * 宽松 JSON 解析器：容忍大模型常见的非法输出（字符串内未转义的换行/控制字符、
     * 单引号、未加引号的键、尾随逗号、注释等）。星火 HTTP 接口无 json_object 模式，
     * 长文本讲解极易产生此类瑕疵，严格解析失败后用它兜底重试。
     */
    private final ObjectMapper lenientJsonMapper;
    @Autowired(required = false)
    private AgentRunLogRepository agentRunLogRepository;
    @Autowired(required = false)
    private PromptTemplateService promptTemplateService;

    protected BaseAgent(LlmService llmService, ObjectMapper objectMapper) {
        this.llmService = llmService;
        this.objectMapper = objectMapper;
        this.lenientJsonMapper = objectMapper.copy()
                .configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true)
                .configure(JsonReadFeature.ALLOW_SINGLE_QUOTES.mappedFeature(), true)
                .configure(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES.mappedFeature(), true)
                .configure(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature(), true)
                .configure(JsonReadFeature.ALLOW_JAVA_COMMENTS.mappedFeature(), true);
    }

    // ------------------------------------------------------------------ 模板方法

    /**
     * 执行 Agent 主流程（模板方法，子类不应覆盖）。
     *
     * @param context 当前协作上下文
     * @return 执行结果，包含结构化数据、触发事件及下一步建议
     */
    public AgentResponse execute(AgentContext context) {
        long startedAt = System.currentTimeMillis();
        String prompt = "";
        String llmResponse = "";
        try {
            log.info("[{}] 开始执行, studentId={}", getName(), context.getStudentId());

            // Step 1: 组装提示词
            prompt = buildPrompt(context);

            // Step 2: 调用 LLM
            llmResponse = callLLM(prompt, context);

            // Step 3: 解析 LLM 输出
            Map<String, Object> parsed = parseResponse(llmResponse, context);

            // Step 4: 质量校验并构造最终响应
            AgentResponse response = qualityCheck(parsed, context);

            saveRunLog(context, prompt, llmResponse, response,
                    System.currentTimeMillis() - startedAt, null);
            log.info("[{}] 执行完成, event={}, message={}", getName(), response.getEvent(), response.getMessage());
            return response;

        } catch (Exception e) {
            log.error("[{}] 执行失败: {}", getName(), e.getMessage(), e);
            saveRunLog(context, prompt, llmResponse, null,
                    System.currentTimeMillis() - startedAt, e);
            return AgentResponse.failure("Agent执行失败: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ 抽象方法（子类必须实现）

    /**
     * 返回 Agent 的显示名称，用于日志和系统提示词生成。
     * 例如："诊断Agent"、"溯源Agent"。
     */
    protected abstract String getName();

    /**
     * 根据当前上下文构造发送给 LLM 的用户侧提示词。
     *
     * @param context 当前协作上下文
     * @return 完整的用户提示词字符串
     */
    protected abstract String buildPrompt(AgentContext context);

    /**
     * 将 LLM 返回的原始文本解析为结构化键值对。
     * 实现时应处理 JSON 提取、字段校验等逻辑。
     *
     * @param llmResponse LLM 返回的原始文本
     * @param context     当前协作上下文（可用于辅助解析）
     * @return 解析后的结构化数据
     */
    protected abstract Map<String, Object> parseResponse(String llmResponse, AgentContext context);

    /**
     * 对 {@link #parseResponse} 的结果进行质量校验，决定是否接受、拒绝或降级。
     * 校验通过后将数据封装为 {@link AgentResponse} 返回。
     *
     * @param parsed  解析后的结构化数据
     * @param context 当前协作上下文
     * @return 最终 AgentResponse
     */
    protected abstract AgentResponse qualityCheck(Map<String, Object> parsed, AgentContext context);

    // ------------------------------------------------------------------ 可覆盖的钩子方法

    /**
     * 调用 LLM 并返回原始文本（同步阻塞）。
     * 子类可覆盖此方法以实现重试、缓存等策略。
     *
     * @param prompt  用户提示词
     * @param context 当前上下文（供子类按需扩展使用）
     * @return LLM 原始输出文本
     */
    protected String callLLM(String prompt, AgentContext context) {
        return llmService.chatSync(prompt, getSystemPrompt(), getTemperature());
    }

    /**
     * 系统提示词，注入 Agent 的角色定义和输出格式要求。
     * 子类可覆盖以提供更精确的角色描述。
     */
    protected String getSystemPrompt() {
        return "你是SmartMentor智学导师系统的" + getName()
                + "，专门负责高校课程个性化学习辅助。请严格按照要求的JSON格式输出结果。";
    }

    /**
     * LLM 采样温度（0.0 ~ 1.0）。
     * 默认 0.7；对于需要精确结构化输出的 Agent，建议子类覆盖为更低值（如 0.3）。
     */
    protected double getTemperature() {
        return 0.7;
    }

    /**
     * Prompt version written to AgentRunLog. Override when a concrete Agent
     * externalizes or substantially changes its prompt contract.
     */
    protected String getPromptVersion() {
        return loadPromptTemplate(getPromptTemplateKey(), "inline-v1", "").getVersion();
    }

    protected String getPromptTemplateKey() {
        return "";
    }

    protected PromptTemplateService.PromptTemplate loadPromptTemplate(String key,
                                                                      String fallbackVersion,
                                                                      String fallbackContent) {
        if (key == null || key.isBlank() || promptTemplateService == null) {
            return new PromptTemplateService.PromptTemplate(fallbackVersion, fallbackContent);
        }
        return promptTemplateService.load(key, fallbackVersion, fallbackContent);
    }

    // ------------------------------------------------------------------ 工具方法（供子类使用）

    /**
     * 向提示词追加一行学生画像信息（值非空时）。各内容生成 Agent 共用，避免重复实现。
     *
     * @param sb      目标 StringBuilder
     * @param profile 学生画像 Map（可为 null）
     * @param key     画像字段名
     * @param label   展示标签
     */
    protected void appendProfileLine(StringBuilder sb, Map<String, Object> profile, String key, String label) {
        if (profile == null) {
            return;
        }
        Object v = profile.get(key);
        if (v != null && !String.valueOf(v).isBlank() && !"null".equals(String.valueOf(v))) {
            sb.append("- ").append(label).append("：").append(v).append("\n");
        }
    }

    /**
     * 从 LLM 原始输出中提取第一个完整、括号平衡的 JSON 值（对象 {...} 或数组 [...]）。
     * <p>
     * 相比简单的 {@code indexOf('{')}/{@code lastIndexOf('}')} 粗切，本实现通过括号配对扫描
     * 找到第一个起始括号对应的真正闭合位置，并正确跳过字符串字面量内的括号与转义字符，
     * 从而避免：LLM 回显示例 JSON 后再给真实答案时截到错误片段、或解释文字中的 {} 干扰。
     *
     * @param raw LLM 原始输出
     * @return 提取出的 JSON 字符串，若未找到平衡结构则返回清理后的原文
     */
    protected String extractJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return "{}";
        }
        // 去除 markdown 代码块标记
        String cleaned = raw.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();

        // 找到第一个 { 或 [ 作为起点
        int start = -1;
        char open = 0, close = 0;
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (c == '{') { start = i; open = '{'; close = '}'; break; }
            if (c == '[') { start = i; open = '['; close = ']'; break; }
        }
        if (start == -1) {
            return cleaned;
        }

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == open) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    return cleaned.substring(start, i + 1);
                }
            }
        }
        // 未找到平衡的闭合括号（可能被截断），回退到原有粗切兜底
        int lastClose = cleaned.lastIndexOf(close);
        if (lastClose > start) {
            return cleaned.substring(start, lastClose + 1);
        }
        return cleaned;
    }

    /**
     * 安全地将 LLM 原始 JSON 输出解析为 Map，解析失败时返回空 Map。
     *
     * @param llmResponse LLM 原始输出
     * @return 解析结果，出错时为空 Map
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> safeParseJson(String llmResponse) {
        String json = extractJson(llmResponse);
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception strictError) {
            // 星火等无 json 模式的接口在长文本里常产生未转义控制字符等瑕疵，用宽松解析兜底
            try {
                Map<String, Object> result = lenientJsonMapper.readValue(json, Map.class);
                log.info("[{}] 严格JSON解析失败，宽松解析成功兜底", getName());
                return result;
            } catch (Exception lenientError) {
                log.warn("[{}] JSON解析失败（严格+宽松均失败），原始内容: {}", getName(), llmResponse, lenientError);
                return new java.util.HashMap<>();
            }
        }
    }

    private void saveRunLog(AgentContext context,
                            String prompt,
                            String llmResponse,
                            AgentResponse response,
                            long latencyMs,
                            Exception error) {
        if (agentRunLogRepository == null) {
            return;
        }
        try {
            AgentRunLog runLog = new AgentRunLog();
            runLog.setAgentName(getName());
            runLog.setStudentId(context != null ? context.getStudentId() : null);
            runLog.setDiagnosticId(context != null ? context.getDiagnosticId() : null);
            runLog.setModule(context != null ? context.getModule() : null);
            runLog.setPromptHash(sha256(prompt));
            runLog.setPromptVersion(getPromptVersion());
            runLog.setPromptLength(prompt != null ? prompt.length() : 0);
            runLog.setResponseLength(llmResponse != null ? llmResponse.length() : 0);
            runLog.setModel(llmService != null ? llmService.getModel() : null);
            runLog.setLatencyMs(latencyMs);
            runLog.setSuccess(response != null && response.isSuccess());
            runLog.setFallbackUsed(resolveFallbackUsed(context, response, error));
            runLog.setQualityScore(resolveQualityScore(response, error));
            runLog.setEvent(response != null && response.getEvent() != null ? response.getEvent().name() : null);
            runLog.setMessage(response != null ? truncate(response.getMessage(), 500) : null);
            runLog.setInputSummary(buildInputSummary(context, prompt));
            runLog.setOutputSummary(buildOutputSummary(response, llmResponse, error));
            runLog.setErrorMessage(error != null ? truncate(error.getMessage(), 2000) : null);
            agentRunLogRepository.save(runLog);
        } catch (Exception logError) {
            log.warn("[{}] Agent运行日志保存失败: {}", getName(), logError.getMessage());
        }
    }

    private String sha256(String text) {
        if (text == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private Boolean resolveFallbackUsed(AgentContext context, AgentResponse response, Exception error) {
        if (error != null || response == null || !response.isSuccess()) {
            return true;
        }
        Object fallbackFromData = response.getData() != null ? response.getData().get("fallbackUsed") : null;
        if (fallbackFromData instanceof Boolean) {
            return (Boolean) fallbackFromData;
        }
        Object fallbackFromContext = context != null && context.getSessionData() != null
                ? context.getSessionData().get("fallbackUsed")
                : null;
        if (fallbackFromContext instanceof Boolean) {
            return (Boolean) fallbackFromContext;
        }
        return false;
    }

    /**
     * 计算写入 {@code agent_run_log.quality_score} 的分值。
     * <p>
     * 注意：除非 Agent 在 data 里显式给出 {@code qualityScore}，否则此处仅是<b>完整度启发值</b>
     * （非空字段占比映射到 0.65~1.0），反映"产出字段填得多不多"，并不评估内容正确性/贴题性。
     * 真正的质量校验由各 Agent 的 qualityCheck 承担。
     */
    private Double resolveQualityScore(AgentResponse response, Exception error) {
        if (error != null || response == null) {
            return 0.0;
        }
        Object explicitScore = response.getData() != null ? response.getData().get("qualityScore") : null;
        if (explicitScore instanceof Number) {
            return clampScore(((Number) explicitScore).doubleValue());
        }
        if (!response.isSuccess()) {
            return 0.0;
        }
        Map<String, Object> data = response.getData();
        if (data == null || data.isEmpty()) {
            return 0.6;
        }
        int nonEmptyFields = 0;
        for (Object value : data.values()) {
            if (isMeaningfulValue(value)) {
                nonEmptyFields++;
            }
        }
        double completeness = Math.min(1.0, nonEmptyFields / Math.max(1.0, data.size()));
        return clampScore(0.65 + completeness * 0.35);
    }

    private Double clampScore(double score) {
        return Math.max(0.0, Math.min(1.0, score));
    }

    private boolean isMeaningfulValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String) {
            return !((String) value).isBlank();
        }
        if (value instanceof Map) {
            return !((Map<?, ?>) value).isEmpty();
        }
        if (value instanceof Iterable) {
            return ((Iterable<?>) value).iterator().hasNext();
        }
        return true;
    }

    private String buildInputSummary(AgentContext context, String prompt) {
        if (context == null) {
            return truncate("promptLength=" + (prompt != null ? prompt.length() : 0), 1000);
        }
        StringBuilder summary = new StringBuilder();
        summary.append("studentId=").append(context.getStudentId())
                .append(", module=").append(context.getModule())
                .append(", diagnosticId=").append(context.getDiagnosticId())
                .append(", currentKnowledgePoint=").append(context.getCurrentKnowledgePoint())
                .append(", promptLength=").append(prompt != null ? prompt.length() : 0);

        if (context.getSessionData() != null && !context.getSessionData().isEmpty()) {
            summary.append(", sessionKeys=").append(context.getSessionData().keySet());
        }
        if (context.getWeakPoints() != null && !context.getWeakPoints().isEmpty()) {
            summary.append(", weakPoints=").append(context.getWeakPoints().size());
        }
        if (context.getKnowledgeMastery() != null && !context.getKnowledgeMastery().isEmpty()) {
            summary.append(", masteryPoints=").append(context.getKnowledgeMastery().size());
        }
        return truncate(summary.toString(), 1000);
    }

    private String buildOutputSummary(AgentResponse response, String llmResponse, Exception error) {
        if (error != null) {
            return truncate("error=" + error.getMessage(), 1000);
        }
        if (response == null) {
            return truncate("llmResponseLength=" + (llmResponse != null ? llmResponse.length() : 0), 1000);
        }
        StringBuilder summary = new StringBuilder();
        summary.append("success=").append(response.isSuccess())
                .append(", event=").append(response.getEvent())
                .append(", message=").append(response.getMessage())
                .append(", dataKeys=").append(response.getData() != null ? response.getData().keySet() : List.of());

        String keyCounts = summarizeCollectionFields(response.getData());
        if (!keyCounts.isEmpty()) {
            summary.append(", ").append(keyCounts);
        }
        return truncate(summary.toString(), 1000);
    }

    private String summarizeCollectionFields(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return "";
        }
        return data.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof List || entry.getValue() instanceof Map)
                .map(entry -> entry.getKey() + "Size=" + collectionSize(entry.getValue()))
                .collect(Collectors.joining(", "));
    }

    private int collectionSize(Object value) {
        if (value instanceof List) {
            return ((List<?>) value).size();
        }
        if (value instanceof Map) {
            return ((Map<?, ?>) value).size();
        }
        return 0;
    }
}
