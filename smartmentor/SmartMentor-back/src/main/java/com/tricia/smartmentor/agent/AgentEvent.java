package com.tricia.smartmentor.agent;

/**
 * 多 Agent 协作框架中流转的事件类型。
 * 每个枚举值对应 Agent 执行完毕后可以触发的一种状态变更，
 * AgentOrchestrator 根据事件类型路由到对应的下游处理器。
 */
public enum AgentEvent {

    /** 诊断 Agent 完成知识薄弱点识别 */
    DIAGNOSIS_COMPLETE,

    /** 溯源 Agent 完成根因知识点追踪 */
    TRACING_COMPLETE,

    /** 规划 Agent 生成学习路径 */
    PATH_GENERATED,

    /** 教学 Agent 生成课程/练习内容 */
    LESSON_GENERATED,

    /** 资源 Agent 生成多模态学习资源（思维导图/拓展阅读/实操案例/动画脚本等） */
    RESOURCE_GENERATED,

    /** 评估 Agent 判定学生未达掌握标准，需继续学习 */
    MASTERY_NOT_REACHED,

    /** 评估 Agent 判定学生已掌握当前知识点 */
    MASTERY_REACHED,

    /** 检测到学生在同一题型连续出错 */
    CONSECUTIVE_ERRORS,

    /** 在教学过程中发现新的薄弱点 */
    NEW_WEAKNESS_FOUND,

    /** 溯源发现跨模块的根本原因 */
    CROSS_MODULE_ROOT_FOUND,

    /** 前置知识点已被学生掌握，可推进主线学习 */
    PREREQUISITE_MASTERED
}
