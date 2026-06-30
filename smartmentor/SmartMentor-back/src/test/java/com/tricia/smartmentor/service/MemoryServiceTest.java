package com.tricia.smartmentor.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * MemoryService 相似度计算单元测试（纯 POJO，覆盖余弦与关键词兜底）。
 * 对应 docs/MEMORY_DESIGN.md §9.6 测试约束。
 */
class MemoryServiceTest {

    @Test
    void cosine_identicalVectorIsOne() {
        float[] v = {1f, 2f, 3f};
        Assertions.assertEquals(1.0, MemoryService.cosine(v, v), 1e-6);
    }

    @Test
    void cosine_orthogonalIsZero() {
        Assertions.assertEquals(0.0, MemoryService.cosine(new float[]{1, 0}, new float[]{0, 1}), 1e-6);
    }

    @Test
    void cosine_differentLengthRejected() {
        // 不同维度向量不可比，返回 0（绝不抛错或误算）
        Assertions.assertEquals(0.0, MemoryService.cosine(new float[]{1, 2, 3}, new float[]{1, 2}), 1e-9);
    }

    @Test
    void cosine_zeroVectorIsZero() {
        Assertions.assertEquals(0.0, MemoryService.cosine(new float[]{0, 0}, new float[]{1, 1}), 1e-9);
    }

    @Test
    void keyword_substringIsTop() {
        Assertions.assertEquals(1.0, MemoryService.keywordScore("反向传播", "对反向传播的链式法则反复混淆"), 1e-9);
    }

    @Test
    void keyword_unrelatedIsLow() {
        double s = MemoryService.keywordScore("数字电路触发器", "学生偏好看图表动画");
        Assertions.assertTrue(s < 0.3, "无关内容关键词分应很低，实际=" + s);
    }

    @Test
    void keyword_orderingMakesRelatedRankHigher() {
        String query = "机器学习训练集测试集";
        double related = MemoryService.keywordScore(query, "对机器学习训练集和测试集的划分不清楚");
        double unrelated = MemoryService.keywordScore(query, "目标是考研上岸");
        Assertions.assertTrue(related > unrelated, "相关条目应排在无关条目之前");
    }
}
