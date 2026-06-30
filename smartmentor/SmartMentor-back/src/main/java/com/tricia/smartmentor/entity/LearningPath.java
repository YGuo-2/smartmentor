package com.tricia.smartmentor.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "learning_path")
public class LearningPath {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "tracing_result_id")
    private Long tracingResultId;

    @Column(name = "target_knowledge_point_id", length = 100)
    private String targetKnowledgePointId;

    @Column(name = "target_knowledge_point_name", length = 100)
    private String targetKnowledgePointName;

    @Column(name = "root_cause_point_id", length = 100)
    private String rootCausePointId;

    @Column(name = "root_cause_point_name", length = 100)
    private String rootCausePointName;

    @Column(name = "path_name", length = 200)
    private String pathName;

    @Column(name = "current_node_id", length = 100)
    private String currentNodeId;

    @Column(length = 20)
    private String mode;

    @Column(length = 20)
    private String status; // active, completed, paused

    @Column(precision = 3, scale = 2)
    private BigDecimal progress;

    @Column(name = "total_estimated_minutes")
    private Integer totalEstimatedMinutes;

    @Column(name = "actual_study_minutes")
    private Integer actualStudyMinutes;

    @Column(name = "total_nodes")
    private Integer totalNodes;

    @Column(name = "completed_nodes")
    private Integer completedNodes;

    @Column(columnDefinition = "JSON")
    private String nodes;

    @Column(name = "tracing_path", columnDefinition = "JSON")
    private String tracingPath;

    @Column(name = "lesson_snapshots", columnDefinition = "JSON")
    private String lessonSnapshots;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_study_at")
    private LocalDateTime lastStudyAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = "active";
        if (progress == null) progress = BigDecimal.ZERO;
        if (actualStudyMinutes == null) actualStudyMinutes = 0;
        if (completedNodes == null) completedNodes = 0;
        createdAt = LocalDateTime.now();
    }
}
