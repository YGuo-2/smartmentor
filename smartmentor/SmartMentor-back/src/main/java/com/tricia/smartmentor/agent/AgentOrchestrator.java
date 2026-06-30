package com.tricia.smartmentor.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 事件驱动的多 Agent 编排器。
 * <p>
 * 使用方式：
 * <pre>
 *   // 1. 注册事件处理器（通常在配置类或初始化方法中完成）
 *   orchestrator.on(AgentEvent.DIAGNOSIS_COMPLETE, ctx -> tracingAgent.execute(ctx));
 *   orchestrator.on(AgentEvent.TRACING_COMPLETE,   ctx -> planningAgent.execute(ctx));
 *
 *   // 2. 手动触发事件
 *   orchestrator.fireEvent(AgentEvent.DIAGNOSIS_COMPLETE, context);
 *
 *   // 3. 或者直接以线性 Pipeline 模式运行
 *   orchestrator.executePipeline(context, diagAgent, tracingAgent, planningAgent);
 * </pre>
 */
@Slf4j
@Component
public class AgentOrchestrator {

    /**
     * 事件 -> 处理器列表的映射。
     * 同一事件可注册多个处理器，按注册顺序依次执行。
     */
    private final Map<AgentEvent, List<Function<AgentContext, AgentResponse>>> eventHandlers =
            new EnumMap<>(AgentEvent.class);

    /**
     * 防止事件链路无限递归的最大触发轮次。
     * 超过此值后，即使 AgentResponse 携带新事件，也不再级联触发。
     */
    private static final int MAX_COLLABORATION_ROUNDS = 10;

    // ------------------------------------------------------------------ 注册 API

    /**
     * 注册事件处理器。
     * 同一事件可多次调用以注册多个处理器（按注册顺序执行）。
     *
     * @param event   触发条件
     * @param handler 处理函数，接收上下文并返回 AgentResponse
     */
    public void on(AgentEvent event, Function<AgentContext, AgentResponse> handler) {
        eventHandlers.computeIfAbsent(event, k -> new ArrayList<>()).add(handler);
    }

    /**
     * 注销指定事件的所有处理器。
     *
     * @param event 要清除处理器的事件
     */
    public void off(AgentEvent event) {
        eventHandlers.remove(event);
    }

    // ------------------------------------------------------------------ 事件触发

    /**
     * 触发指定事件，依次执行所有注册的处理器，并支持级联事件传播。
     * <p>
     * 级联规则：
     * <ul>
     *   <li>若某处理器返回的 {@code AgentResponse} 中携带非 null 的 {@code event} 字段，
     *       且当前已触发事件总数未超过 {@link #MAX_COLLABORATION_ROUNDS}，
     *       则自动触发该后续事件。</li>
     *   <li>为防止同一事件重复触发导致死循环，已在 {@link AgentContext#getEvents()} 中
     *       记录历史，调用方可通过 {@link AgentContext#hasEventFired} 自行判断。</li>
     * </ul>
     *
     * @param event   要触发的事件
     * @param context 当前协作上下文（会被修改：events 列表会追加本次事件）
     * @return 本次事件及所有级联事件产生的 AgentResponse 列表
     */
    public List<AgentResponse> fireEvent(AgentEvent event, AgentContext context) {
        log.info("触发事件: {}, studentId={}, 已触发轮次={}", event, context.getStudentId(), context.getEvents().size());
        context.getEvents().add(event);

        List<Function<AgentContext, AgentResponse>> handlers = eventHandlers.get(event);
        if (handlers == null || handlers.isEmpty()) {
            log.debug("事件 {} 无注册处理器，跳过", event);
            return Collections.emptyList();
        }

        List<AgentResponse> responses = new ArrayList<>();

        for (Function<AgentContext, AgentResponse> handler : handlers) {
            AgentResponse response = handler.apply(context);
            responses.add(response);

            // 将处理器产出数据合并进上下文会话数据
            if (response.getData() != null && !response.getData().isEmpty()) {
                context.getSessionData().putAll(response.getData());
            }

            // 级联事件：未超过最大轮次且携带新事件
            if (response.getEvent() != null
                    && context.getEvents().size() < MAX_COLLABORATION_ROUNDS) {
                log.debug("事件 {} 触发级联事件 {}", event, response.getEvent());
                List<AgentResponse> cascaded = fireEvent(response.getEvent(), context);
                responses.addAll(cascaded);
            } else if (response.getEvent() != null) {
                log.warn("已达最大协作轮次 {}，跳过级联事件 {}", MAX_COLLABORATION_ROUNDS, response.getEvent());
            }
        }

        return responses;
    }

    // ------------------------------------------------------------------ Pipeline 模式

    /**
     * 以线性 Pipeline 方式依次执行多个 Agent。
     * <p>
     * 每个 Agent 执行后：
     * <ol>
     *   <li>若失败，Pipeline 立即中断并返回当前上下文（已记录截止状态）。</li>
     *   <li>若成功，将 {@code AgentResponse.data} 合并入 {@code context.sessionData}，
     *       并触发 {@code AgentResponse.event}（如果存在）。</li>
     * </ol>
     *
     * @param context 初始协作上下文
     * @param agents  按执行顺序排列的 Agent 实例
     * @return 执行完毕（或中断）后的最终上下文
     */
    public AgentContext executePipeline(AgentContext context, BaseAgent... agents) {
        log.info("Pipeline 启动: agents={}, studentId={}", agents.length, context.getStudentId());

        for (BaseAgent agent : agents) {
            AgentResponse response = agent.execute(context);

            if (!response.isSuccess()) {
                log.warn("Pipeline 中断于 Agent [{}]: {}", agent.getName(), response.getMessage());
                break;
            }

            // 合并结果数据
            if (response.getData() != null && !response.getData().isEmpty()) {
                context.getSessionData().putAll(response.getData());
            }

            // 触发后续事件（级联由 fireEvent 内部处理）
            if (response.getEvent() != null) {
                fireEvent(response.getEvent(), context);
            }
        }

        log.info("Pipeline 结束: studentId={}, 触发事件={}", context.getStudentId(), context.getEvents());
        return context;
    }

    // ------------------------------------------------------------------ 查询 API

    /**
     * 返回指定事件当前注册的处理器数量，主要用于测试和诊断。
     */
    public int getHandlerCount(AgentEvent event) {
        List<Function<AgentContext, AgentResponse>> handlers = eventHandlers.get(event);
        return handlers == null ? 0 : handlers.size();
    }

    /**
     * 清空所有已注册的事件处理器，主要用于测试场景的重置。
     */
    public void clearAllHandlers() {
        eventHandlers.clear();
        log.debug("已清空所有事件处理器");
    }
}
