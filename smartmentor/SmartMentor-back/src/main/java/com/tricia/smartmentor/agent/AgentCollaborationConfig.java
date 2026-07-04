package com.tricia.smartmentor.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * 多 Agent 事件驱动协作配置。
 * <p>
 * 注册事件处理链路：
 * <pre>
 * DIAGNOSIS_COMPLETE       → TracingAgent（追溯根因）
 * TRACING_COMPLETE         → PlanningAgent（生成学习路径）
 * CROSS_MODULE_ROOT_FOUND  → PlanningAgent（跨模块也走规划）
 * MASTERY_NOT_REACHED      → TeachingAgent（重新教学）
 * MASTERY_REACHED          → 日志记录（节点完成，由 Service 层推进）
 * CONSECUTIVE_ERRORS       → TeachingAgent（即时干预补救）
 * NEW_WEAKNESS_FOUND       → TracingAgent（新薄弱点追溯，预留触发）
 * </pre>
 */
@Slf4j
@Configuration
public class AgentCollaborationConfig {

    private final AgentOrchestrator orchestrator;
    private final DiagnosticAgent diagnosticAgent;
    private final TracingAgent tracingAgent;
    private final PlanningAgent planningAgent;
    private final TeachingAgent teachingAgent;
    private final EvaluationAgent evaluationAgent;

    public AgentCollaborationConfig(AgentOrchestrator orchestrator,
                                    DiagnosticAgent diagnosticAgent,
                                    TracingAgent tracingAgent,
                                    PlanningAgent planningAgent,
                                    TeachingAgent teachingAgent,
                                    EvaluationAgent evaluationAgent) {
        this.orchestrator = orchestrator;
        this.diagnosticAgent = diagnosticAgent;
        this.tracingAgent = tracingAgent;
        this.planningAgent = planningAgent;
        this.teachingAgent = teachingAgent;
        this.evaluationAgent = evaluationAgent;
    }

    @PostConstruct
    public void wireEvents() {
        log.info("注册多Agent协作事件处理器...");

        // 诊断完成 → 追溯根因
        orchestrator.on(AgentEvent.DIAGNOSIS_COMPLETE, ctx -> {
            log.info("[协作] 诊断完成，启动根因追溯 studentId={}", ctx.getStudentId());
            return tracingAgent.execute(ctx);
        });

        // 溯源完成 → 规划学习路径
        orchestrator.on(AgentEvent.TRACING_COMPLETE, ctx -> {
            log.info("[协作] 溯源完成，启动路径规划 studentId={}", ctx.getStudentId());
            return planningAgent.execute(ctx);
        });

        // 跨模块根因 → 同样走路径规划（会自动包含跨模块节点）
        orchestrator.on(AgentEvent.CROSS_MODULE_ROOT_FOUND, ctx -> {
            log.info("[协作] 发现跨模块根因，启动路径规划 studentId={}", ctx.getStudentId());
            return planningAgent.execute(ctx);
        });

        // 掌握度未达标 → 重新生成教学内容（降低难度）
        orchestrator.on(AgentEvent.MASTERY_NOT_REACHED, ctx -> {
            log.info("[协作] 掌握度未达标，降难度重新生成教学内容 studentId={}", ctx.getStudentId());
            // 降低当前节点的掌握度估计，让 TeachingAgent 落入更基础的教学策略（<0.4 触发 foundation 策略）
            Object m = ctx.getSessionData().get("masteryLevel");
            double cur = (m instanceof Number) ? ((Number) m).doubleValue() : 0.5;
            ctx.putSessionData("masteryLevel", Math.max(0.0, cur - 0.2));
            ctx.putSessionData("contentScope", TeachingAgent.SCOPE_FULL);
            return teachingAgent.execute(ctx);
        });

        // 掌握度达标 → 记录日志（节点推进由 LearningService 管理）
        orchestrator.on(AgentEvent.MASTERY_REACHED, ctx -> {
            log.info("[协作] 知识点已掌握 studentId={}, 当前路径将推进到下一节点", ctx.getStudentId());
            return AgentResponse.success("知识点掌握确认", null);
        });

        // 连续错误 → 触发教学干预
        orchestrator.on(AgentEvent.CONSECUTIVE_ERRORS, ctx -> {
            log.info("[协作] 检测到连续错误，启动教学干预 studentId={}", ctx.getStudentId());
            Object m = ctx.getSessionData().get("masteryLevel");
            double cur = (m instanceof Number) ? ((Number) m).doubleValue() : 0.5;
            ctx.putSessionData("masteryLevel", Math.min(cur, 0.35));
            ctx.putSessionData("contentScope", TeachingAgent.SCOPE_EXPLAIN);
            return teachingAgent.execute(ctx);
        });

        // 发现新薄弱点 → 追溯
        // 预留链路：当前尚无端点在教学过程中检出新薄弱点并 fire 此事件，注册保留待后续接入。
        orchestrator.on(AgentEvent.NEW_WEAKNESS_FOUND, ctx -> {
            log.info("[协作] 发现新薄弱点，启动追溯 studentId={}", ctx.getStudentId());
            return tracingAgent.execute(ctx);
        });

        log.info("多Agent协作事件处理器注册完成，共注册 {} 类事件",
                AgentEvent.values().length);
    }
}
