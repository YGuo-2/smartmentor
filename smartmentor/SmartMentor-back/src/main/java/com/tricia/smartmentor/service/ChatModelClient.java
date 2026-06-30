package com.tricia.smartmentor.service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 大模型底层提供商客户端的统一抽象。
 * <p>
 * 每个具体实现对接一家大模型服务（如讯飞星火、DeepSeek），向上层暴露一致的
 * 同步 / 流式对话能力，便于在 {@link LlmService}（大模型适配/路由层）中按
 * 配置切换与回退，从而满足"AI 能力优先使用科大讯飞"的要求，同时保留备用通道。
 */
public interface ChatModelClient {

    /** 提供商标识，如 spark / deepseek。 */
    String providerName();

    /** 是否已正确配置（如 api-key 非空），未配置的提供商会被路由层跳过。 */
    boolean isConfigured();

    /** 当前使用的模型名称。 */
    String getModel();

    /**
     * 流式对话。回调在 HTTP 线程执行，调用方需注意线程安全。
     */
    void streamChat(List<Map<String, String>> messages,
                    double temperature,
                    Consumer<String> onToken,
                    Runnable onComplete,
                    Consumer<Exception> onError);

    /** 同步对话，返回完整文本。 */
    String chatSync(String userMessage, String systemPrompt, double temperature);

    /** 同步对话（要求模型输出 JSON）。 */
    String chatJsonSync(String userMessage, String systemPrompt, double temperature);

    /** 多轮同步对话，传入完整消息列表。 */
    String chatMultiTurnSync(List<Map<String, String>> messages, double temperature);
}
