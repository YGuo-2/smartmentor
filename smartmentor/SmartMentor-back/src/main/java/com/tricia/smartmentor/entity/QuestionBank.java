package com.tricia.smartmentor.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "question_bank")
public class QuestionBank {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_hash", unique = true, nullable = false, length = 64)
    private String questionHash;

    @Column(length = 40)
    private String source;

    @Column(name = "source_ref", length = 100)
    private String sourceRef;

    @Column(length = 50)
    private String module;

    @Column(name = "knowledge_point_id", length = 100)
    private String knowledgePointId;

    @Column(name = "knowledge_point_name", length = 100)
    private String knowledgePointName;

    @Column(name = "question_type", length = 30)
    private String questionType;

    @Column(precision = 4, scale = 2)
    private BigDecimal difficulty;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "JSON")
    private String options;

    @Column(name = "correct_answer", columnDefinition = "TEXT")
    private String correctAnswer;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "error_type", length = 100)
    private String errorType;

    @Column(name = "quality_score", precision = 4, scale = 2)
    private BigDecimal qualityScore;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
