package com.tricia.smartmentor.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "mission")
public class Mission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mission_id", unique = true, length = 100)
    private String missionId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(length = 20)
    private String type; // diagnostic, learning, review

    @Column(name = "reward_exp")
    private Integer rewardExp;

    @Column(length = 20)
    private String status; // pending, completed

    @Column(name = "mission_date")
    private LocalDate missionDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = "pending";
        createdAt = LocalDateTime.now();
    }
}
