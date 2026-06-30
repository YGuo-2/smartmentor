package com.tricia.smartmentor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurrentUserResponse {
    private Long userId;
    private String username;
    private String nickname;
    private String email;
    private String role;
    private String grade;
    private String school;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private ProfileSummary profile;
    private Long classCount;
    private Long studentCount;

    @Data
    @Builder
    public static class ProfileSummary {
        private BigDecimal overallMastery;
        private Integer level;
        private Integer experiencePoints;
        private Integer streakDays;
        private BigDecimal totalStudyHours;
        private LocalDateTime lastDiagnosticAt;
        private String majorDirection;
        private String educationLevel;
        private String currentCourse;
        private String learningGoal;
        private String foundationLevel;
        private String resourcePreference;
        private String academicInterest;
    }
}
