package com.tricia.smartmentor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * DeepSeek 大模型客户端（OpenAI 兼容 HTTP 接口）。
 * <p>
 * 作为备用通道：当主用提供商（讯飞星火）未配置或调用失败时由路由层回退到此。
 */
@Component
public class DeepSeekChatClient extends AbstractOpenAiChatClient {

    public DeepSeekChatClient(@Value("${deepseek.api-key:}") String apiKey,
                              @Value("${deepseek.base-url:https://api.deepseek.com}") String baseUrl,
                              @Value("${deepseek.model:deepseek-chat}") String model,
                              ObjectMapper objectMapper) {
        super("deepseek", apiKey, baseUrl, model, true, objectMapper);
    }
}
