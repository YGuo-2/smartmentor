package com.tricia.smartmentor.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 协作上下文。
 * 在整个诊断 → 溯源 → 规划 → 教学 → 评估的 Pipeline 中，
 * 各 Agent 通过读写同一个 AgentContext 实例来传递状态。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentContext {

    // ------------------------------------------------------------------ 学生基本信息
    /** 学生 ID */
    private Long studentId;

    /** 当前课程模块（如 "机器学习"、"Java Web"） */
    private String module;

    // ------------------------------------------------------------------ 诊断阶段
    /** 诊断会话 ID */
    private String diagnosticId;

    /**
     * 诊断识别出的薄弱点列表。
     * 每条记录至少包含：kpId（知识点ID）、kpName（知识点名称）、masteryLevel（掌握度 0-1）
     */
    @Builder.Default
    private List<Map<String, Object>> weakPoints = new ArrayList<>();

    /**
     * 各知识点的掌握度映射：kpId -> 掌握度（0.0 ~ 1.0）。
     */
    @Builder.Default
    private Map<String, Double> knowledgeMastery = new HashMap<>();

    // ------------------------------------------------------------------ 溯源阶段
    /**
     * 错误模式统计：errorType -> 出现次数。
     * errorType 例如 "符号运算错误"、"公式记忆错误" 等。
     */
    @Builder.Default
    private Map<String, Integer> errorPatterns = new HashMap<>();

    /**
     * 溯源分析得出的根因知识点 ID 列表（按重要性降序排列）。
     */
    @Builder.Default
    private List<String> rootCauses = new ArrayList<>();

    // ------------------------------------------------------------------ 规划阶段
    /**
     * 学习路径：有序的知识点 ID 列表，学生需按此顺序学习。
     */
    @Builder.Default
    private List<String> learningPath = new ArrayList<>();

    /** 当前正在学习的知识点 ID */
    private String currentKnowledgePoint;

    // ------------------------------------------------------------------ 学生画像
    /**
     * 学生画像，包含 majorDirection、educationLevel、currentCourse、learningGoal 等。
     */
    @Builder.Default
    private Map<String, Object> studentProfile = new HashMap<>();

    // ------------------------------------------------------------------ 会话临时数据
    /**
     * 当前会话的临时数据（不跨 Pipeline 持久化），供 Agent 之间传递中间结果。
     */
    @Builder.Default
    private Map<String, Object> sessionData = new HashMap<>();

    // ------------------------------------------------------------------ 事件历史
    /**
     * 本次 Pipeline 执行过程中已触发的事件历史，用于防止循环触发和审计追踪。
     */
    @Builder.Default
    private List<AgentEvent> events = new ArrayList<>();

    // ------------------------------------------------------------------ 辅助方法

    /**
     * 浅拷贝当前上下文（集合字段做一层拷贝，避免共享引用）。
     * 用于需要分叉执行时创建独立副本。
     */
    public AgentContext cloneContext() {
        return AgentContext.builder()
                .studentId(this.studentId)
                .module(this.module)
                .diagnosticId(this.diagnosticId)
                .weakPoints(new ArrayList<>(this.weakPoints))
                .knowledgeMastery(new HashMap<>(this.knowledgeMastery))
                .errorPatterns(new HashMap<>(this.errorPatterns))
                .rootCauses(new ArrayList<>(this.rootCauses))
                .learningPath(new ArrayList<>(this.learningPath))
                .currentKnowledgePoint(this.currentKnowledgePoint)
                .studentProfile(new HashMap<>(this.studentProfile))
                .sessionData(new HashMap<>(this.sessionData))
                .events(new ArrayList<>(this.events))
                .build();
    }

    /**
     * 便捷方法：向 sessionData 写入单个键值对。
     */
    public AgentContext putSessionData(String key, Object value) {
        this.sessionData.put(key, value);
        return this;
    }

    /**
     * 便捷方法：更新指定知识点的掌握度。
     */
    public AgentContext updateMastery(String kpId, double mastery) {
        this.knowledgeMastery.put(kpId, mastery);
        return this;
    }

    /**
     * 便捷方法：判断某事件是否已经在本次 Pipeline 中触发过。
     */
    public boolean hasEventFired(AgentEvent event) {
        return this.events.contains(event);
    }
}
