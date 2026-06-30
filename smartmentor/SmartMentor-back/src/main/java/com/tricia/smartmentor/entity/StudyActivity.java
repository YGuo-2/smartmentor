package com.tricia.smartmentor.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "study_activity")
public class StudyActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "activity_type", length = 30)
    private String activityType; // diagnostic, lesson, exercise, chat

    @Column(length = 200)
    private String description;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "result_summary", length = 500)
    private String resultSummary;

    @Column(name = "knowledge_point_id", length = 100)
    private String knowledgePointId;

    @Column(name = "module", length = 50)
    private String module;

    @Column(name = "activity_date")
    private LocalDate activityDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (activityDate == null) activityDate = LocalDate.now();
        createdAt = LocalDateTime.now();
    }
}
