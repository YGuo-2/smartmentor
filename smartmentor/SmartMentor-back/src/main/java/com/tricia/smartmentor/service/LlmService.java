package com.tricia.smartmentor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 大模型适配 / 路由层（保持原类名以兼容既有调用方）。
 * <p>
 * 按配置 {@code llm.provider} 选择主用大模型提供商，默认 <b>DeepSeek</b>（长内容生成更快更稳）；
 * 当主用提供商未配置或同步调用失败时，在开启回退（{@code llm.fallback-enabled}）的情况下
 * 自动切换到备用提供商（讯飞星火 spark），既保证多智能体程序稳定可用，
 * 也满足赛题"AI 能力使用科大讯飞"的要求。
 * <p>
 * 离线演示模式（{@code smartmentor.demo.offline-agent-enabled}）下直接返回本地构造的
 * 响应，便于无网络/无密钥环境下的比赛演示。
 */
@Slf4j
@Service
public class LlmService {

    private final SparkChatClient sparkChatClient;
    private final DeepSeekChatClient deepSeekChatClient;
    private final OfflineDemoService offlineDemoService;
    private final boolean offlineDemoEnabled;
    private final String provider;
    private final boolean fallbackEnabled;

    public LlmService(SparkChatClient sparkChatClient,
                           DeepSeekChatClient deepSeekChatClient,
                           OfflineDemoService offlineDemoService,
                           @Value("${smartmentor.demo.offline-agent-enabled:false}") boolean offlineDemoEnabled,
                           @Value("${llm.provider:deepseek}") String provider,
                           @Value("${llm.fallback-enabled:true}") boolean fallbackEnabled) {
        this.sparkChatClient = sparkChatClient;
        this.deepSeekChatClient = deepSeekChatClient;
        this.offlineDemoService = offlineDemoService;
        this.offlineDemoEnabled = offlineDemoEnabled;
        this.provider = provider == null ? "deepseek" : provider.trim().toLowerCase();
        this.fallbackEnabled = fallbackEnabled;
        log.info("LLM 适配层初始化：provider={}, fallbackEnabled={}, sparkConfigured={}, deepseekConfigured={}",
                this.provider, fallbackEnabled, sparkChatClient.isConfigured(), deepSeekChatClient.isConfigured());
    }

    /** 主用客户端：按配置选择，若主用未配置而备用已配置则自动改用备用。 */
    private ChatModelClient primaryClient() {
        ChatModelClient preferred = "deepseek".equals(provider) ? deepSeekChatClient : sparkChatClient;
        if (!preferred.isConfigured()) {
            ChatModelClient alternative = preferred == sparkChatClient ? deepSeekChatClient : sparkChatClient;
            if (alternative.isConfigured()) {
                return alternative;
            }
        }
        return preferred;
    }

    /** 备用客户端（与主用相对的另一家）。 */
    private ChatModelClient secondaryClient(ChatModelClient primary) {
        return primary == sparkChatClient ? deepSeekChatClient : sparkChatClient;
    }

    public String getModel() {
        return primaryClient().getModel();
    }

    public void streamChat(List<Map<String, String>> messages,
                           double temperature,
                           Consumer<String> onToken,
                           Runnable onComplete,
                           Consumer<Exception> onError) {
        if (offlineDemoEnabled) {
            try {
                log.info("离线演示模式：返回本地流式响应");
                String response = offlineDemoService.buildChatResponse(messages);
                for (String chunk : response.split("(?<=。|\\n)")) {
                    if (!chunk.isEmpty()) {
                        onToken.accept(chunk);
                    }
                }
                onComplete.run();
            } catch (Exception e) {
                onError.accept(e);
            }
            return;
        }
        ChatModelClient primary = primaryClient();
        streamWithFallback(primary, messages, temperature, onToken, onComplete, onError);
    }

    /**
     * 流式调用主用提供商；若在尚未吐出任何 token 之前失败，且开启回退、备用已配置，
     * 则自动切换到备用提供商重新发起一次流式请求。
     * <p>
     * 一旦已经向前端推送过 token，再失败则不再回退（避免重复/拼接错乱的内容），
     * 直接把错误交给上层处理。
     */
    private void streamWithFallback(ChatModelClient primary,
                                    List<Map<String, String>> messages,
                                    double temperature,
                                    Consumer<String> onToken,
                                    Runnable onComplete,
                                    Consumer<Exception> onError) {
        java.util.concurrent.atomic.AtomicBoolean tokenEmitted =
                new java.util.concurrent.atomic.AtomicBoolean(false);
        Consumer<String> wrappedOnToken = token -> {
            tokenEmitted.set(true);
            onToken.accept(token);
        };
        Consumer<Exception> wrappedOnError = error -> {
            ChatModelClient secondary = secondaryClient(primary);
            if (!tokenEmitted.get() && fallbackEnabled && secondary.isConfigured()) {
                log.warn("主用大模型[{}]流式调用失败（未输出 token），回退到[{}]：{}",
                        primary.providerName(), secondary.providerName(), error.getMessage());
                secondary.streamChat(messages, temperature, onToken, onComplete, onError);
            } else {
                onError.accept(error);
            }
        };
        primary.streamChat(messages, temperature, wrappedOnToken, onComplete, wrappedOnError);
    }

    public String chatSync(String userMessage, String systemPrompt, double temperature) {
        if (offlineDemoEnabled) {
            log.info("离线演示模式：返回本地 Agent 响应");
            return offlineDemoService.buildAgentResponse(systemPrompt, userMessage);
        }
        return callSyncWithFallback(client -> client.chatSync(userMessage, systemPrompt, temperature));
    }

    public String chatJsonSync(String userMessage, String systemPrompt, double temperature) {
        if (offlineDemoEnabled) {
            log.info("离线演示模式：返回本地 JSON Agent 响应");
            return offlineDemoService.buildAgentResponse(systemPrompt, userMessage);
        }
        return callSyncWithFallback(client -> client.chatJsonSync(userMessage, systemPrompt, temperature));
    }

    public String chatMultiTurnSync(List<Map<String, String>> messages, double temperature) {
        if (offlineDemoEnabled) {
            log.info("离线演示模式：返回本地多轮对话响应");
            return offlineDemoService.buildChatResponse(messages);
        }
        return callSyncWithFallback(client -> client.chatMultiTurnSync(messages, temperature));
    }

    /**
     * 同步调用主用提供商；失败且开启回退、备用已配置时，切换到备用提供商再试一次。
     */
    private String callSyncWithFallback(java.util.function.Function<ChatModelClient, String> call) {
        ChatModelClient primary = primaryClient();
        try {
            return call.apply(primary);
        } catch (RuntimeException e) {
            ChatModelClient secondary = secondaryClient(primary);
            if (fallbackEnabled && secondary.isConfigured()) {
                log.warn("主用大模型[{}]调用失败，回退到[{}]：{}",
                        primary.providerName(), secondary.providerName(), e.getMessage());
                return call.apply(secondary);
            }
            throw e;
        }
    }
}
