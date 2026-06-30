package com.tricia.smartmentor.agent;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 编排器单元测试（纯 POJO，无 Spring 上下文）。
 * 重点验证 fireEventNoCascade 触发处理器但不级联下游事件。
 */
class AgentOrchestratorTest {

    @Test
    void fireEventRunsCascadeChain() {
        AgentOrchestrator orchestrator = new AgentOrchestrator();
        // DIAGNOSIS_COMPLETE 的处理器产出 TRACING_COMPLETE
        orchestrator.on(AgentEvent.DIAGNOSIS_COMPLETE,
                ctx -> AgentResponse.success("诊断", null, AgentEvent.TRACING_COMPLETE));
        boolean[] downstreamRan = {false};
        orchestrator.on(AgentEvent.TRACING_COMPLETE, ctx -> {
            downstreamRan[0] = true;
            return AgentResponse.success("溯源", null);
        });

        AgentContext ctx = AgentContext.builder().studentId(1L).build();
        orchestrator.fireEvent(AgentEvent.DIAGNOSIS_COMPLETE, ctx);

        Assertions.assertTrue(downstreamRan[0], "普通 fireEvent 应级联触发下游 TRACING_COMPLETE 处理器");
    }

    @Test
    void fireEventNoCascadeRunsHandlerButSkipsCascade() {
        AgentOrchestrator orchestrator = new AgentOrchestrator();
        boolean[] handlerRan = {false};
        boolean[] downstreamRan = {false};
        orchestrator.on(AgentEvent.DIAGNOSIS_COMPLETE, ctx -> {
            handlerRan[0] = true;
            return AgentResponse.success("诊断", null, AgentEvent.TRACING_COMPLETE);
        });
        orchestrator.on(AgentEvent.TRACING_COMPLETE, ctx -> {
            downstreamRan[0] = true;
            return AgentResponse.success("溯源", null);
        });

        AgentContext ctx = AgentContext.builder().studentId(1L).build();
        orchestrator.fireEventNoCascade(AgentEvent.DIAGNOSIS_COMPLETE, ctx);

        Assertions.assertTrue(handlerRan[0], "NoCascade 应执行本事件的处理器");
        Assertions.assertFalse(downstreamRan[0], "NoCascade 不应级联触发下游处理器");
    }

    @Test
    void fireEventNoCascadeReturnsHandlerData() {
        AgentOrchestrator orchestrator = new AgentOrchestrator();
        orchestrator.on(AgentEvent.DIAGNOSIS_COMPLETE,
                ctx -> AgentResponse.success("ok", java.util.Map.of("k", "v")));

        AgentContext ctx = AgentContext.builder().studentId(1L).build();
        var responses = orchestrator.fireEventNoCascade(AgentEvent.DIAGNOSIS_COMPLETE, ctx);

        Assertions.assertEquals(1, responses.size());
        Assertions.assertEquals("v", responses.get(0).getData().get("k"));
    }
}
