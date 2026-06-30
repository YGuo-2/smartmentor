package com.tricia.smartmentor.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "mastery_history")
public class MasteryHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "knowledge_point_id", length = 100)
    private String knowledgePointId;

    @Column(name = "module", length = 50)
    private String module;

    @Column(name = "mastery", precision = 5, scale = 4)
    private BigDecimal mastery;

    @Column(name = "overall_mastery", precision = 5, scale = 4)
    private BigDecimal overallMastery;

    @Column(name = "source", length = 30)
    private String source; // diagnostic, practice, teaching, chat

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
