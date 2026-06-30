package com.tricia.smartmentor.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tracing_result")
public class TracingResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracing_id", unique = true, nullable = false, length = 100)
    private String tracingId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "diagnostic_id", length = 100)
    private String diagnosticId;

    @Column(name = "analyzed_point_count")
    private Integer analyzedPointCount;

    @Column(name = "root_cause_count")
    private Integer rootCauseCount;

    @Column(name = "is_cross_module")
    private Boolean isCrossModule;

    @Column(name = "tracing_results", columnDefinition = "JSON")
    private String tracingResults;

    @Column(name = "merged_root_causes", columnDefinition = "JSON")
    private String mergedRootCauses;

    @Column(name = "graph_visualization", columnDefinition = "JSON")
    private String graphVisualization;

    @Column(name = "suggested_learning_path", columnDefinition = "JSON")
    private String suggestedLearningPath;

    @Column(columnDefinition = "TEXT")
    private String suggestion;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
