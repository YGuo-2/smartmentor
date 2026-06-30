package com.tricia.smartmentor.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "student_profile")
public class StudentProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "learning_style", length = 20)
    private String learningStyle;

    @Column(name = "daily_study_minutes")
    private Integer dailyStudyMinutes;

    @Column(name = "preferred_time_slot", length = 20)
    private String preferredTimeSlot;

    @Column(name = "target_school", length = 100)
    private String targetSchool;

    @Column(name = "target_score")
    private Integer targetScore;

    @Column(name = "major_direction", length = 50)
    private String majorDirection;

    @Column(name = "education_level", length = 20)
    private String educationLevel;

    @Column(name = "current_course", length = 100)
    private String currentCourse;

    @Column(name = "learning_goal", length = 50)
    private String learningGoal;

    @Column(name = "foundation_level", length = 20)
    private String foundationLevel;

    @Column(name = "resource_preference", columnDefinition = "JSON")
    private String resourcePreference;

    @Column(name = "academic_interest", length = 255)
    private String academicInterest;

    @Column(name = "weak_module_priority", columnDefinition = "JSON")
    private String weakModulePriority;

    @Column(name = "study_mode", length = 20)
    private String studyMode;

    @Column(name = "overall_mastery", precision = 3, scale = 2)
    private BigDecimal overallMastery;

    @Column(name = "ability_param", precision = 5, scale = 2)
    private BigDecimal abilityParam;

    @Column(name = "error_patterns", columnDefinition = "JSON")
    private String errorPatterns;

    @Column(name = "knowledge_state_json", columnDefinition = "JSON")
    private String knowledgeState;

    @Column(name = "streak_days")
    private Integer streakDays;

    @Column(name = "total_study_hours", precision = 10, scale = 1)
    private BigDecimal totalStudyHours;

    @Column
    private Integer level;

    @Column(name = "experience_points")
    private Integer experiencePoints;

    @Column(name = "last_diagnostic_at")
    private LocalDateTime lastDiagnosticAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (overallMastery == null) overallMastery = BigDecimal.ZERO;
        if (abilityParam == null) abilityParam = BigDecimal.ZERO;
        if (streakDays == null) streakDays = 0;
        if (totalStudyHours == null) totalStudyHours = BigDecimal.ZERO;
        if (level == null) level = 1;
        if (experiencePoints == null) experiencePoints = 0;
        if (dailyStudyMinutes == null) dailyStudyMinutes = 45;
        if (learningStyle == null) learningStyle = "visual";
        if (preferredTimeSlot == null) preferredTimeSlot = "evening";
        if (studyMode == null) studyMode = "systematic";
        if (majorDirection == null) majorDirection = "计算机类";
        if (educationLevel == null) educationLevel = "本科";
        if (currentCourse == null) currentCourse = "人工智能基础";
        if (learningGoal == null) learningGoal = "项目实践";
        if (foundationLevel == null) foundationLevel = "基础";
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
