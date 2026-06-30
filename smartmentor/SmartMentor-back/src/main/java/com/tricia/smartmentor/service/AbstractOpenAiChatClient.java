package com.tricia.smartmentor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.BufferedSource;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * OpenAI 兼容 Chat Completions 接口的通用客户端实现。
 * <p>
 * 讯飞星火、DeepSeek 等均提供 {@code POST /v1/chat/completions} 的 OpenAI 兼容接口
 * （Bearer 鉴权、messages 数组、SSE 流式 delta.content），故抽到此基类统一实现，
 * 子类只需提供各自的 apiKey / baseUrl / model 等配置。
 */
@Slf4j
public abstract class AbstractOpenAiChatClient implements ChatModelClient {

    // 长输出场景（8 题诊断含选项、逐题详细分析、多节点学习路径）4096 易截断，上调留足空间。
    private static final int MAX_TOKENS = 8192;
    private static final int SYNC_MAX_RETRIES = 2;

    private final String providerName;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    /** 是否支持 response_format=json_object（部分提供商不支持，关闭后靠提示词约束 JSON）。 */
    private final boolean jsonModeSupported;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    protected AbstractOpenAiChatClient(String providerName,
                                       String apiKey,
                                       String baseUrl,
                                       String model,
                                       boolean jsonModeSupported,
                                       ObjectMapper objectMapper) {
        this.providerName = providerName;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.model = model;
        this.jsonModeSupported = jsonModeSupported;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                // 整次调用的硬超时（含连接/写/读/重定向）。同步 execute() 不响应线程中断，
                // 上层 future.cancel(true) 无法中断阻塞的 socket 读，故在此设硬上限兜底，避免线程被钉死。
                .callTimeout(190, TimeUnit.SECONDS)
                .build();
    }

    private static String normalizeBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    @PreDestroy
    public void destroy() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }

    @Override
    public String providerName() {
        return providerName;
    }

    @Override
    public boolean isConfigured() {
        return !apiKey.isEmpty() && !baseUrl.isEmpty();
    }

    @Override
    public String getModel() {
        return model;
    }

    private String completionsUrl() {
        return baseUrl + "/v1/chat/completions";
    }

    @Override
    public void streamChat(List<Map<String, String>> messages,
                           double temperature,
                           Consumer<String> onToken,
                           Runnable onComplete,
                           Consumer<Exception> onError) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("stream", true);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", MAX_TOKENS);

            ArrayNode messagesNode = requestBody.putArray("messages");
            for (Map<String, String> msg : messages) {
                ObjectNode msgNode = messagesNode.addObject();
                msgNode.put("role", msg.get("role"));
                msgNode.put("content", msg.get("content"));
            }

            String json = objectMapper.writeValueAsString(requestBody);
            log.debug("[{}] stream 请求: messages={} 条, model={}", providerName, messages.size(), model);

            Request request = new Request.Builder()
                    .url(completionsUrl())
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "text/event-stream")
                    .post(RequestBody.create(json, MediaType.get("application/json")))
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    log.error("[{}] API 连接失败: {}", providerName, e.getMessage());
                    onError.accept(e);
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try (ResponseBody body = response.body()) {
                        if (!response.isSuccessful()) {
                            String errorBody = body != null ? body.string() : "unknown";
                            log.error("[{}] API 错误 {}: {}", providerName, response.code(), errorBody);
                            onError.accept(new IOException("API " + response.code() + ": " + errorBody));
                            return;
                        }
                        if (body == null) {
                            onError.accept(new IOException("Response body is null"));
                            return;
                        }

                        BufferedSource source = body.source();
                        while (!source.exhausted()) {
                            String line = source.readUtf8Line();
                            if (line == null) break;
                            if (line.isEmpty()) continue;
                            if (!line.startsWith("data:")) continue;

                            String data = line.substring(5).trim();
                            if ("[DONE]".equals(data)) {
                                break;
                            }

                            try {
                                JsonNode chunk = objectMapper.readTree(data);
                                JsonNode choices = chunk.get("choices");
                                if (choices != null && choices.size() > 0) {
                                    JsonNode delta = choices.get(0).get("delta");
                                    if (delta != null && delta.has("content")) {
                                        String content = delta.get("content").asText("");
                                        if (!content.isEmpty()) {
                                            onToken.accept(content);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.debug("[{}] 解析 chunk 跳过: {}", providerName, data);
                            }
                        }
                        onComplete.run();
                    } catch (Exception e) {
                        log.error("[{}] 读取流响应异常: {}", providerName, e.getMessage());
                        onError.accept(e);
                    }
                }
            });
        } catch (Exception e) {
            log.error("[{}] 构建请求失败: {}", providerName, e.getMessage());
            onError.accept(e);
        }
    }

    @Override
    public String chatSync(String userMessage, String systemPrompt, double temperature) {
        return chatSyncWithRetry(userMessage, systemPrompt, temperature, false, SYNC_MAX_RETRIES);
    }

    @Override
    public String chatJsonSync(String userMessage, String systemPrompt, double temperature) {
        return chatSyncWithRetry(userMessage, systemPrompt, temperature, true, SYNC_MAX_RETRIES);
    }

    private String chatSyncWithRetry(String userMessage, String systemPrompt,
                                     double temperature, boolean jsonMode, int maxRetries) {
        Exception lastException = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                if (attempt > 0) {
                    log.info("[{}] API 重试第 {} 次", providerName, attempt);
                    Thread.sleep(1000L * attempt);
                }
                return doSyncCall(userMessage, systemPrompt, temperature, jsonMode);
            } catch (LlmApiException e) {
                lastException = e;
                log.warn("[{}] API 调用失败 (attempt {}, status={}): {}", providerName, attempt + 1, e.getStatusCode(), e.getMessage());
                if (!e.isRetryable()) {
                    // 确定性错误（4xx，如密钥/参数错误）：重试无意义，立即抛出供上层跳过回退
                    throw e;
                }
            } catch (Exception e) {
                lastException = e;
                log.warn("[{}] API 调用失败 (attempt {}): {}", providerName, attempt + 1, e.getMessage());
            }
        }
        throw new RuntimeException(providerName + " API 调用失败，已重试 " + maxRetries + " 次: "
                + (lastException != null ? lastException.getMessage() : "unknown"), lastException);
    }

    private String doSyncCall(String userMessage, String systemPrompt,
                              double temperature, boolean jsonMode) throws IOException {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.put("stream", false);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", MAX_TOKENS);

        if (jsonMode && jsonModeSupported) {
            ObjectNode responseFormat = objectMapper.createObjectNode();
            responseFormat.put("type", "json_object");
            requestBody.set("response_format", responseFormat);
        }

        ArrayNode messagesNode = requestBody.putArray("messages");
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            ObjectNode sysMsg = messagesNode.addObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
        }
        ObjectNode userMsg = messagesNode.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);

        String json = objectMapper.writeValueAsString(requestBody);
        log.debug("[{}] sync 请求: model={}, jsonMode={}", providerName, model, jsonMode && jsonModeSupported);

        Request request = new Request.Builder()
                .url(completionsUrl())
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(json, MediaType.get("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (!response.isSuccessful()) {
                String errorBody = body != null ? body.string() : "unknown";
                throw new LlmApiException(response.code(),
                        LlmApiException.isRetryableStatus(response.code()),
                        providerName + " API error " + response.code() + ": " + errorBody);
            }
            if (body == null) {
                throw new IOException("Response body is null");
            }

            String responseStr = body.string();
            JsonNode responseJson = objectMapper.readTree(responseStr);
            JsonNode choices = responseJson.get("choices");
            if (choices != null && choices.size() > 0) {
                JsonNode firstChoice = choices.get(0);
                // M4：输出因长度上限被截断时按可重试错误处理，避免截断的半截 JSON 被静默当成成功结果
                JsonNode finishReason = firstChoice.get("finish_reason");
                if (finishReason != null && "length".equals(finishReason.asText())) {
                    throw new LlmApiException(0, true,
                            providerName + " 输出因 max_tokens 截断 (finish_reason=length)，结果不完整");
                }
                JsonNode message = firstChoice.get("message");
                if (message != null && message.has("content")) {
                    return message.get("content").asText("");
                }
            }
            throw new IOException("Invalid response structure: " + responseStr);
        }
    }

    @Override
    public String chatMultiTurnSync(List<Map<String, String>> messages, double temperature) {
        Exception lastException = null;
        for (int attempt = 0; attempt <= SYNC_MAX_RETRIES; attempt++) {
            try {
                if (attempt > 0) {
                    log.info("[{}] 多轮 API 重试第 {} 次", providerName, attempt);
                    Thread.sleep(1000L * attempt);
                }
                return doMultiTurnCall(messages, temperature);
            } catch (LlmApiException e) {
                lastException = e;
                log.warn("[{}] 多轮 API 调用失败 (attempt {}, status={}): {}", providerName, attempt + 1, e.getStatusCode(), e.getMessage());
                if (!e.isRetryable()) {
                    throw e;
                }
            } catch (Exception e) {
                lastException = e;
                log.warn("[{}] 多轮 API 调用失败 (attempt {}): {}", providerName, attempt + 1, e.getMessage());
            }
        }
        throw new RuntimeException(providerName + " multi-turn call failed, retried " + SYNC_MAX_RETRIES
                + " times: " + (lastException != null ? lastException.getMessage() : "unknown"), lastException);
    }

    private String doMultiTurnCall(List<Map<String, String>> messages, double temperature) throws IOException {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.put("stream", false);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", MAX_TOKENS);

        ArrayNode messagesNode = requestBody.putArray("messages");
        for (Map<String, String> msg : messages) {
            ObjectNode msgNode = messagesNode.addObject();
            msgNode.put("role", msg.get("role"));
            msgNode.put("content", msg.get("content"));
        }

        String json = objectMapper.writeValueAsString(requestBody);
        Request request = new Request.Builder()
                .url(completionsUrl())
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(json, MediaType.get("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (!response.isSuccessful()) {
                String errorBody = body != null ? body.string() : "unknown";
                throw new LlmApiException(response.code(),
                        LlmApiException.isRetryableStatus(response.code()),
                        providerName + " API error " + response.code() + ": " + errorBody);
            }
            if (body == null) throw new IOException("null body");

            JsonNode responseJson = objectMapper.readTree(body.string());
            JsonNode choices = responseJson.get("choices");
            if (choices != null && choices.size() > 0) {
                JsonNode firstChoice = choices.get(0);
                JsonNode finishReason = firstChoice.get("finish_reason");
                if (finishReason != null && "length".equals(finishReason.asText())) {
                    throw new LlmApiException(0, true,
                            providerName + " 多轮输出因 max_tokens 截断，结果不完整");
                }
                return firstChoice.get("message").get("content").asText("");
            }
            throw new IOException("No choices in response");
        }
    }
}
