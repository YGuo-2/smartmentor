package com.tricia.smartmentor.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "answer_record")
public class AnswerRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "diagnostic_id", length = 100)
    private String diagnosticId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "question_index")
    private Integer questionIndex;

    @Column(name = "question_id")
    private Long questionId;

    @Column(name = "knowledge_point_id", length = 100)
    private String knowledgePointId;

    @Column(name = "knowledge_point_name", length = 100)
    private String knowledgePointName;

    @Column(name = "question_type", length = 20)
    private String questionType;

    @Column(precision = 3, scale = 2)
    private BigDecimal difficulty;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "JSON")
    private String options;

    @Column(name = "student_answer", columnDefinition = "TEXT")
    private String studentAnswer;

    @Column(name = "correct_answer", length = 500)
    private String correctAnswer;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "time_spent")
    private Integer timeSpent;

    @Column(name = "error_type", length = 50)
    private String errorType;

    @Column(name = "error_detail", length = 200)
    private String errorDetail;

    @Column(name = "error_analysis", columnDefinition = "TEXT")
    private String errorAnalysis;

    @Column(columnDefinition = "TEXT")
    private String solution;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
