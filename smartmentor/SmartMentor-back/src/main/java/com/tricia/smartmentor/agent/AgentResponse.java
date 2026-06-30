package com.tricia.smartmentor.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Agent 执行结果的统一包装器。
 * <p>
 * 每个 Agent 的 {@code execute()} 方法返回一个 AgentResponse，
 * AgentOrchestrator 根据其中的 {@code event} 字段决定是否触发级联处理，
 * 根据 {@code nextAgent} 字段给出后续调用建议。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResponse {

    /** 执行是否成功 */
    private boolean success;

    /**
     * 结构化数据，Agent 产出的键值对结果（如诊断报告、学习路径等）。
     * AgentOrchestrator 会将其合并入 AgentContext#sessionData。
     */
    @Builder.Default
    private Map<String, Object> data = new HashMap<>();

    /**
     * 执行完成后需要触发的事件。
     * 为 null 表示无需触发任何后续事件。
     */
    private AgentEvent event;

    /**
     * 人类可读的执行摘要，用于日志和前端展示。
     */
    private String message;

    /**
     * 建议调用的下一个 Agent 名称（可选）。
     * 为 null 或空字符串表示 Pipeline 已可结束或由编排器自行决定。
     */
    private String nextAgent;

    // ------------------------------------------------------------------ 工厂方法

    /**
     * 创建一个成功响应（不带事件触发）。
     */
    public static AgentResponse success(String message, Map<String, Object> data) {
        return AgentResponse.builder()
                .success(true)
                .message(message)
                .data(data != null ? data : new HashMap<>())
                .build();
    }

    /**
     * 创建一个成功响应并携带后续触发事件。
     */
    public static AgentResponse success(String message, Map<String, Object> data, AgentEvent event) {
        return AgentResponse.builder()
                .success(true)
                .message(message)
                .data(data != null ? data : new HashMap<>())
                .event(event)
                .build();
    }

    /**
     * 创建一个成功响应，携带事件和下一个 Agent 建议。
     */
    public static AgentResponse success(String message, Map<String, Object> data,
                                        AgentEvent event, String nextAgent) {
        return AgentResponse.builder()
                .success(true)
                .message(message)
                .data(data != null ? data : new HashMap<>())
                .event(event)
                .nextAgent(nextAgent)
                .build();
    }

    /**
     * 创建一个失败响应。
     */
    public static AgentResponse failure(String message) {
        return AgentResponse.builder()
                .success(false)
                .message(message)
                .data(new HashMap<>())
                .build();
    }

    /**
     * 创建一个失败响应并附带部分数据（如错误诊断信息）。
     */
    public static AgentResponse failure(String message, Map<String, Object> data) {
        return AgentResponse.builder()
                .success(false)
                .message(message)
                .data(data != null ? data : new HashMap<>())
                .build();
    }
}
