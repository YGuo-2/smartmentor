package com.tricia.smartmentor.service;

/**
 * 文本向量化客户端。把 embedding 抽成接口，使记忆系统不绑定具体提供商：
 * 当前为关键词召回版，无实现 Bean，{@link MemoryService} 以
 * {@code @Autowired(required = false)} 兜底为 null，召回降级为关键词匹配。
 * <p>
 * 接入讯飞向量化时再补 {@code SparkEmbeddingClient}（按 spark.embedding.mode
 * 切 legacy-hmac / maas-openai-compatible），上层无需改动。设计见 docs/MEMORY_DESIGN.md §8。
 */
public interface EmbeddingClient {

    /**
     * 把文本向量化。
     *
     * @return 向量；不可用/失败时返回 null（调用方据此降级，绝不抛错阻断对话）
     */
    float[] embed(String text);

    /** 向量来源标识，写入 student_memory.embedding_provider，如 spark-maas。 */
    String providerName();

    /** 向量模型名，写入 student_memory.embedding_model，用于隔离不可比向量。 */
    String modelName();
}
