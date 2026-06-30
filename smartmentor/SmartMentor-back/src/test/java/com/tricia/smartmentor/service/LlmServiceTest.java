package com.tricia.smartmentor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

class LlmServiceTest {

    @Test
    void fallsBackToSecondaryProviderAfterPrimaryNonRetryableError() {
        ObjectMapper objectMapper = new ObjectMapper();
        FakeSparkClient spark = new FakeSparkClient(objectMapper);
        FakeDeepSeekClient deepSeek = new FakeDeepSeekClient(objectMapper);
        spark.chatSyncFailure = new LlmApiException(400, false, "primary request rejected");
        deepSeek.chatSyncResponse = "secondary-ok";

        LlmService service = new LlmService(
                spark,
                deepSeek,
                new OfflineDemoService(objectMapper),
                false,
                "spark",
                true);

        String result = service.chatSync("hello", "system", 0.3);

        Assertions.assertEquals("secondary-ok", result);
        Assertions.assertEquals(1, spark.chatSyncCalls);
        Assertions.assertEquals(1, deepSeek.chatSyncCalls);
    }

    private static class FakeSparkClient extends SparkChatClient {
        RuntimeException chatSyncFailure;
        String chatSyncResponse = "spark-ok";
        int chatSyncCalls;

        FakeSparkClient(ObjectMapper objectMapper) {
            super("fake-key", "http://localhost", "fake-spark", objectMapper);
        }

        @Override
        public boolean isConfigured() {
            return true;
        }

        @Override
        public String chatSync(String userMessage, String systemPrompt, double temperature) {
            chatSyncCalls++;
            if (chatSyncFailure != null) {
                throw chatSyncFailure;
            }
            return chatSyncResponse;
        }

        @Override
        public String chatJsonSync(String userMessage, String systemPrompt, double temperature) {
            return chatSync(userMessage, systemPrompt, temperature);
        }

        @Override
        public String chatMultiTurnSync(List<Map<String, String>> messages, double temperature) {
            return chatSync("", "", temperature);
        }

        @Override
        public void streamChat(List<Map<String, String>> messages,
                               double temperature,
                               Consumer<String> onToken,
                               Runnable onComplete,
                               Consumer<Exception> onError) {
            onToken.accept(chatSyncResponse);
            onComplete.run();
        }
    }

    private static class FakeDeepSeekClient extends DeepSeekChatClient {
        String chatSyncResponse = "deepseek-ok";
        int chatSyncCalls;

        FakeDeepSeekClient(ObjectMapper objectMapper) {
            super("fake-key", "http://localhost", "fake-deepseek", objectMapper);
        }

        @Override
        public boolean isConfigured() {
            return true;
        }

        @Override
        public String chatSync(String userMessage, String systemPrompt, double temperature) {
            chatSyncCalls++;
            return chatSyncResponse;
        }

        @Override
        public String chatJsonSync(String userMessage, String systemPrompt, double temperature) {
            return chatSync(userMessage, systemPrompt, temperature);
        }

        @Override
        public String chatMultiTurnSync(List<Map<String, String>> messages, double temperature) {
            return chatSync("", "", temperature);
        }

        @Override
        public void streamChat(List<Map<String, String>> messages,
                               double temperature,
                               Consumer<String> onToken,
                               Runnable onComplete,
                               Consumer<Exception> onError) {
            onToken.accept(chatSyncResponse);
            onComplete.run();
        }
    }
}
