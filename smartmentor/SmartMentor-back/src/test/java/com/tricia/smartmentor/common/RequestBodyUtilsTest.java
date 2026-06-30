package com.tricia.smartmentor.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class RequestBodyUtilsTest {

    @Test
    void parsesNumericStringsAndNumbers() {
        Map<String, Object> request = Map.of(
                "pathId", "42",
                "timeSpent", 18.7,
                "threshold", "0.65"
        );

        Assertions.assertEquals(42L, RequestBodyUtils.requiredLong(request, "pathId"));
        Assertions.assertEquals(18, RequestBodyUtils.optionalInteger(request, "timeSpent"));
        Assertions.assertEquals(0.65, RequestBodyUtils.optionalDouble(request, "threshold"));
    }

    @Test
    void treatsBlankNullAndUndefinedAsMissing() {
        Map<String, Object> request = Map.of(
                "blank", " ",
                "nullText", "null",
                "undefinedText", "undefined"
        );

        Assertions.assertNull(RequestBodyUtils.optionalString(request, "blank"));
        Assertions.assertNull(RequestBodyUtils.optionalInteger(request, "nullText"));
        Assertions.assertNull(RequestBodyUtils.optionalDouble(request, "undefinedText"));

        BusinessException e = Assertions.assertThrows(BusinessException.class,
                () -> RequestBodyUtils.requiredLong(request, "undefinedText"));
        Assertions.assertEquals(400, e.getCode());
    }

    @Test
    void rejectsInvalidNumericValuesAsBusinessErrors() {
        Map<String, Object> request = Map.of(
                "questionId", "abc",
                "timeSpent", "12s",
                "threshold", "NaN"
        );

        BusinessException longError = Assertions.assertThrows(BusinessException.class,
                () -> RequestBodyUtils.requiredLong(request, "questionId"));
        Assertions.assertEquals("questionId必须是数字", longError.getMessage());

        BusinessException intError = Assertions.assertThrows(BusinessException.class,
                () -> RequestBodyUtils.optionalInteger(request, "timeSpent"));
        Assertions.assertEquals("timeSpent必须是数字", intError.getMessage());

        BusinessException doubleError = Assertions.assertThrows(BusinessException.class,
                () -> RequestBodyUtils.optionalDouble(request, "threshold"));
        Assertions.assertEquals("threshold必须是数字", doubleError.getMessage());
    }

    @Test
    void parsesStringListAndFiltersEmptyValues() {
        Map<String, Object> request = Map.of(
                "knowledgePointIds", List.of("ai_intro", " ", "undefined", "java_web", 123)
        );

        Assertions.assertEquals(
                List.of("ai_intro", "java_web", "123"),
                RequestBodyUtils.optionalStringList(request, "knowledgePointIds"));
    }

    @Test
    void rejectsNonArrayStringList() {
        Map<String, Object> request = Map.of("knowledgePointIds", "ai_intro");

        BusinessException e = Assertions.assertThrows(BusinessException.class,
                () -> RequestBodyUtils.optionalStringList(request, "knowledgePointIds"));
        Assertions.assertEquals("knowledgePointIds必须是数组", e.getMessage());
    }

    @Test
    void parsesRequiredObjectList() {
        Map<String, Object> request = Map.of(
                "answers", List.of(Map.of("exerciseId", "cp_1", "answer", "A"))
        );

        List<Map<String, Object>> answers = RequestBodyUtils.requiredObjectList(request, "answers");

        Assertions.assertEquals(1, answers.size());
        Assertions.assertEquals("cp_1", answers.get(0).get("exerciseId"));
        Assertions.assertEquals("A", answers.get(0).get("answer"));
    }

    @Test
    void rejectsInvalidObjectListItems() {
        Map<String, Object> request = Map.of("answers", List.of("bad-item"));

        BusinessException e = Assertions.assertThrows(BusinessException.class,
                () -> RequestBodyUtils.requiredObjectList(request, "answers"));
        Assertions.assertEquals("answers的每一项必须是对象", e.getMessage());
    }
}
