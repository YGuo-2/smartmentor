package com.tricia.smartmentor.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生长期记忆条目（情景记忆）。
 * <p>
 * 把跨会话的对话沉淀成一句话事实/偏好/薄弱点，下次对话按相关性召回注入上下文，
 * 解决"换 session 失忆""超窗失忆"。embedding 关键词召回阶段为空，
 * 接入向量化后才填充；embedding_model / embedding_dim 用于隔离不同模型产生的不可比向量。
 * 设计见 docs/MEMORY_DESIGN.md。
 */
@Data
@Entity
@Table(name = "student_memory",
        uniqueConstraints = @UniqueConstraint(name = "uk_student_content",
                columnNames = {"student_id", "content_hash"}))
public class StudentMemory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /** fact / preference / weakness / goal */
    @Column(length = 20, nullable = false)
    private String type;

    /** 一句话记忆，如"对反向传播的链式法则反复混淆" */
    @Column(length = 500, nullable = false)
    private String content;

    /** content 的 SHA-256，去重唯一键用 */
    @Column(name = "content_hash", length = 64, nullable = false)
    private String contentHash;

    /** 向量本体，存为 JSON 字符串（float[]）；null 表示未向量化，走关键词召回 */
    @Column(columnDefinition = "JSON")
    private String embedding;

    /** 向量来自哪套接口，如 spark-maas / spark-hmac */
    @Column(name = "embedding_provider", length = 30)
    private String embeddingProvider;

    /** 向量模型名，模型升级后用于隔离不可比向量 */
    @Column(name = "embedding_model", length = 50)
    private String embeddingModel;

    /** 向量维度，召回时按同维度过滤 */
    @Column(name = "embedding_dim")
    private Integer embeddingDim;

    /** 来源会话，便于溯源 */
    @Column(name = "source_session", length = 100)
    private String sourceSession;

    /** 显著度，留给二期做衰减/巩固 */
    @Column(precision = 3, scale = 2)
    private BigDecimal salience;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (salience == null) salience = BigDecimal.valueOf(0.50);
        createdAt = LocalDateTime.now();
    }
}
