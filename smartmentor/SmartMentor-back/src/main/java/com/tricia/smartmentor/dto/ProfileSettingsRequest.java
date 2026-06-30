package com.tricia.smartmentor.dto;

import lombok.Data;
import java.util.List;

@Data
public class ProfileSettingsRequest {
    private String targetSchool;
    private Integer targetScore;
    private String majorDirection;
    private String educationLevel;
    private String currentCourse;
    private String learningGoal;
    private String foundationLevel;
    private List<String> resourcePreference;
    private String academicInterest;
    private String learningStyle;
    private Integer dailyStudyMinutes;
    private String preferredTimeSlot;
    private List<String> weakModulePriority;
    private String studyMode;
    private String nickname;
    private String avatarUrl;
}
