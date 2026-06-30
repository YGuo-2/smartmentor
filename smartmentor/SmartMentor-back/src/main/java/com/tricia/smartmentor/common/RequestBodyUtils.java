package com.tricia.smartmentor.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RequestBodyUtils {

    private RequestBodyUtils() {
    }

    public static String optionalString(Map<String, Object> request, String fieldName) {
        Object value = request.get(fieldName);
        String text = normalizeText(value);
        return text == null ? null : text;
    }

    public static String requiredString(Map<String, Object> request, String fieldName) {
        String text = optionalString(request, fieldName);
        if (text == null) {
            throw new BusinessException(400, fieldName + "不能为空");
        }
        return text;
    }

    public static Long requiredLong(Map<String, Object> request, String fieldName) {
        Object value = request.get(fieldName);
        String text = normalizeText(value);
        if (text == null) {
            throw new BusinessException(400, fieldName + "不能为空");
        }

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        try {
            return Long.valueOf(text);
        } catch (NumberFormatException e) {
            throw new BusinessException(400, fieldName + "必须是数字");
        }
    }

    public static Integer optionalInteger(Map<String, Object> request, String fieldName) {
        Object value = request.get(fieldName);
        String text = normalizeText(value);
        if (text == null) {
            return null;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException e) {
            throw new BusinessException(400, fieldName + "必须是数字");
        }
    }

    public static Double optionalDouble(Map<String, Object> request, String fieldName) {
        Object value = request.get(fieldName);
        String text = normalizeText(value);
        if (text == null) {
            return null;
        }

        if (value instanceof Number) {
            double number = ((Number) value).doubleValue();
            if (!Double.isFinite(number)) {
                throw new BusinessException(400, fieldName + "必须是数字");
            }
            return number;
        }

        try {
            double number = Double.valueOf(text);
            if (!Double.isFinite(number)) {
                throw new BusinessException(400, fieldName + "必须是数字");
            }
            return number;
        } catch (NumberFormatException e) {
            throw new BusinessException(400, fieldName + "必须是数字");
        }
    }

    public static List<String> optionalStringList(Map<String, Object> request, String fieldName) {
        Object value = request.get(fieldName);
        if (value == null) {
            return null;
        }
        if (!(value instanceof List<?>)) {
            throw new BusinessException(400, fieldName + "必须是数组");
        }

        List<String> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            String text = normalizeText(item);
            if (text != null) {
                result.add(text);
            }
        }
        return result;
    }

    public static List<Map<String, Object>> requiredObjectList(Map<String, Object> request, String fieldName) {
        Object value = request.get(fieldName);
        if (value == null) {
            throw new BusinessException(400, fieldName + "不能为空");
        }
        if (!(value instanceof List<?>)) {
            throw new BusinessException(400, fieldName + "必须是数组");
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            if (!(item instanceof Map<?, ?>)) {
                throw new BusinessException(400, fieldName + "的每一项必须是对象");
            }
            Map<String, Object> normalized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) item).entrySet()) {
                if (entry.getKey() != null) {
                    normalized.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            result.add(normalized);
        }
        if (result.isEmpty()) {
            throw new BusinessException(400, fieldName + "不能为空");
        }
        return result;
    }

    private static String normalizeText(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        if (text.isEmpty()
                || "undefined".equalsIgnoreCase(text)
                || "null".equalsIgnoreCase(text)) {
            return null;
        }
        return text;
    }
}
