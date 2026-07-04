package com.tricia.smartmentor.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

class LearningServicePracticeStateTest {

    private static final LocalDateTime PRACTICED_AT = LocalDateTime.of(2026, 7, 4, 10, 30);

    @Test
    void wrongAnswerIncrementsConsecutiveErrorCount() {
        Map<String, Object> node = new LinkedHashMap<>();

        LearningService.PracticeStateUpdate update = LearningService.updatePracticeState(
                node, false, "ex_1", PRACTICED_AT, 3);

        Assertions.assertEquals(0, update.getPreviousConsecutiveErrorCount());
        Assertions.assertEquals(1, update.getConsecutiveErrorCount());
        Assertions.assertFalse(update.isInterventionTriggered());
        Assertions.assertEquals(1, node.get("consecutiveErrorCount"));
        Assertions.assertEquals(false, node.get("lastPracticeCorrect"));
        Assertions.assertEquals("ex_1", node.get("lastPracticeExerciseId"));
        Assertions.assertEquals(PRACTICED_AT.toString(), node.get("lastPracticeAt"));
    }

    @Test
    void correctAnswerClearsConsecutiveErrorCount() {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("consecutiveErrorCount", 2);

        LearningService.PracticeStateUpdate update = LearningService.updatePracticeState(
                node, true, "ex_2", PRACTICED_AT, 3);

        Assertions.assertEquals(2, update.getPreviousConsecutiveErrorCount());
        Assertions.assertEquals(0, update.getConsecutiveErrorCount());
        Assertions.assertFalse(update.isInterventionTriggered());
        Assertions.assertEquals(0, node.get("consecutiveErrorCount"));
        Assertions.assertEquals(true, node.get("lastPracticeCorrect"));
    }

    @Test
    void thirdConsecutiveWrongAnswerTriggersIntervention() {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("consecutiveErrorCount", 2);

        LearningService.PracticeStateUpdate update = LearningService.updatePracticeState(
                node, false, "ex_3", PRACTICED_AT, 3);

        Assertions.assertEquals(3, update.getConsecutiveErrorCount());
        Assertions.assertTrue(update.isInterventionTriggered());
        Assertions.assertEquals(3, node.get("consecutiveErrorCount"));
    }

    @Test
    void wrongAnswerAfterThresholdDoesNotRetriggerSameCrossing() {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("consecutiveErrorCount", 3);

        LearningService.PracticeStateUpdate update = LearningService.updatePracticeState(
                node, false, "ex_4", PRACTICED_AT, 3);

        Assertions.assertEquals(4, update.getConsecutiveErrorCount());
        Assertions.assertFalse(update.isInterventionTriggered());
        Assertions.assertEquals(4, node.get("consecutiveErrorCount"));
    }
}
