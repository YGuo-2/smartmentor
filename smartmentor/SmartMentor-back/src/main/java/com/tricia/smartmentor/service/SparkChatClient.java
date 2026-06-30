package com.tricia.smartmentor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 讯飞星火大模型客户端（OpenAI 兼容 HTTP 接口）。
 * <p>
 * 端点：{@code https://spark-api-open.xf-yun.com/v1/chat/completions}；
 * 鉴权使用控制台的 HTTP "APIPassword" 作为 Bearer Token。
 * 星火 HTTP 接口默认不开启 response_format=json_object，故 jsonModeSupported=false，
 * JSON 输出依靠提示词约束。
 */
@Component
public class SparkChatClient extends AbstractOpenAiChatClient {

    public SparkChatClient(@Value("${spark.api-key:}") String apiKey,
                           @Value("${spark.base-url:https://spark-api-open.xf-yun.com}") String baseUrl,
                           @Value("${spark.model:4.0Ultra}") String model,
                           ObjectMapper objectMapper) {
        super("spark", apiKey, baseUrl, model, false, objectMapper);
    }
}
