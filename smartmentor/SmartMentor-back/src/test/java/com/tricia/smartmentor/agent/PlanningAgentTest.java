package com.tricia.smartmentor.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tricia.smartmentor.service.KnowledgeGraphService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class PlanningAgentTest {

    private static TestablePlanningAgent agent;

    @BeforeAll
    static void setUp() {
        KnowledgeGraphService knowledgeGraphService = new KnowledgeGraphService(
                new PathMatchingResourcePatternResolver(), new ObjectMapper());
        knowledgeGraphService.init();
        agent = new TestablePlanningAgent(knowledgeGraphService);
    }

    @Test
    void validTopologicalPathPasses() {
        AgentResponse response = agent.check(List.of("ai_intro", "ai_ml_basic", "ai_neural_network"));

        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals(AgentEvent.PATH_GENERATED, response.getEvent());
    }

    @Test
    void unknownGraphNodeFails() {
        AgentResponse response = agent.check(List.of("ai_intro", "not_in_graph", "ai_ml_basic"));

        Assertions.assertFalse(response.isSuccess());
        Assertions.assertTrue(response.getMessage().contains("不在知识图谱"));
    }

    @Test
    void reversedPrerequisiteFails() {
        AgentResponse response = agent.check(List.of("ai_ml_basic", "ai_intro", "ai_neural_network"));

        Assertions.assertFalse(response.isSuccess());
        Assertions.assertTrue(response.getMessage().contains("顺序倒置"));
    }

    @Test
    void lessThanThreeNodesFails() {
        AgentResponse response = agent.check(List.of("ai_intro", "ai_ml_basic"));

        Assertions.assertFalse(response.isSuccess());
        Assertions.assertTrue(response.getMessage().contains("至少需要3个节点"));
    }

    @Test
    void duplicateNodeFails() {
        AgentResponse response = agent.check(List.of("ai_intro", "ai_ml_basic", "ai_ml_basic"));

        Assertions.assertFalse(response.isSuccess());
        Assertions.assertTrue(response.getMessage().contains("重复知识点"));
    }

    @Test
    void blankNodeFails() {
        AgentResponse response = agent.check(new ArrayList<>(List.of("ai_intro", " ", "ai_ml_basic")));

        Assertions.assertFalse(response.isSuccess());
        Assertions.assertTrue(response.getMessage().contains("空知识点ID"));
    }

    private static class TestablePlanningAgent extends PlanningAgent {
        TestablePlanningAgent(KnowledgeGraphService knowledgeGraphService) {
            super(null, new ObjectMapper(), knowledgeGraphService);
        }

        AgentResponse check(List<String> learningPath) {
            List<String> mutableLearningPath = new ArrayList<>(learningPath);
            AgentContext context = AgentContext.builder().studentId(1L).build();
            context.setLearningPath(mutableLearningPath);
            return qualityCheck(Map.of("learningPath", mutableLearningPath), context);
        }
    }
}
