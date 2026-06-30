package com.tricia.smartmentor.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "diagnostic_session")
public class DiagnosticSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "diagnostic_id", unique = true, nullable = false, length = 100)
    private String diagnosticId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(length = 20)
    private String module;

    @Column(length = 20)
    private String status; // in_progress, completed

    @Column(name = "total_questions")
    private Integer totalQuestions;

    @Column(name = "correct_count")
    private Integer correctCount;

    @Column(precision = 3, scale = 2)
    private BigDecimal accuracy;

    @Column(name = "overall_mastery", precision = 3, scale = 2)
    private BigDecimal overallMastery;

    @Column(name = "current_difficulty", precision = 3, scale = 2)
    private BigDecimal currentDifficulty;

    @Column(name = "current_question_index")
    private Integer currentQuestionIndex;

    @Column(name = "knowledge_point_results", columnDefinition = "JSON")
    private String knowledgePointResults;

    @Column(name = "weak_points", columnDefinition = "JSON")
    private String weakPoints;

    @Column(name = "error_patterns", columnDefinition = "JSON")
    private String errorPatterns;

    @Column(name = "question_snapshots", columnDefinition = "JSON")
    private String questionSnapshots;

    @Column(columnDefinition = "TEXT")
    private String suggestion;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = "in_progress";
        if (totalQuestions == null) totalQuestions = 0;
        if (correctCount == null) correctCount = 0;
        if (currentQuestionIndex == null) currentQuestionIndex = 0;
        startTime = LocalDateTime.now();
    }
}
