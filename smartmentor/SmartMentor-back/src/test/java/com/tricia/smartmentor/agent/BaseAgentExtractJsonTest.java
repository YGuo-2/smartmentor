package com.tricia.smartmentor.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * BaseAgent.extractJson 括号配对扫描单元测试（纯 POJO）。
 */
class BaseAgentExtractJsonTest {

    /** 暴露 protected extractJson/safeParseJson 供测试调用的最小子类。 */
    private static class TestAgent extends BaseAgent {
        TestAgent() {
            super(null, new ObjectMapper());
        }
        @Override protected String getName() { return "测试Agent"; }
        @Override protected String buildPrompt(AgentContext context) { return ""; }
        @Override protected Map<String, Object> parseResponse(String r, AgentContext c) { return Map.of(); }
        @Override protected AgentResponse qualityCheck(Map<String, Object> p, AgentContext c) { return AgentResponse.success("", null); }
        String pub(String raw) { return extractJson(raw); }
        Map<String, Object> parse(String raw) { return safeParseJson(raw); }
    }

    private final TestAgent agent = new TestAgent();

    @Test
    void extractsObjectFromMarkdownFence() {
        String raw = "这是分析结果：\n```json\n{\"score\": 80}\n```\n以上。";
        Assertions.assertEquals("{\"score\": 80}", agent.pub(raw));
    }

    @Test
    void picksFirstBalancedObjectNotLastBrace() {
        // 真实答案对象后跟解释文字里又出现 } —— lastIndexOf 会截错，括号配对扫描应只取第一个完整对象
        String raw = "{\"a\": {\"nested\": 1}} 说明：见上 } 结束";
        Assertions.assertEquals("{\"a\": {\"nested\": 1}}", agent.pub(raw));
    }

    @Test
    void ignoresBracesInsideStrings() {
        String raw = "{\"text\": \"包含 } 和 { 的字符串\", \"n\": 1}";
        Map<String, Object> parsed = agent.parse(raw);
        Assertions.assertEquals("包含 } 和 { 的字符串", parsed.get("text"));
        Assertions.assertEquals(1, ((Number) parsed.get("n")).intValue());
    }

    @Test
    void extractsJsonArray() {
        String raw = "结果如下 [{\"id\":\"q1\"},{\"id\":\"q2\"}] 完毕";
        Assertions.assertEquals("[{\"id\":\"q1\"},{\"id\":\"q2\"}]", agent.pub(raw));
    }

    @Test
    void blankReturnsEmptyObject() {
        Assertions.assertEquals("{}", agent.pub("   "));
    }
}
