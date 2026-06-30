package com.tricia.smartmentor.service;

import com.tricia.smartmentor.common.BusinessException;
import com.tricia.smartmentor.entity.Mission;
import com.tricia.smartmentor.entity.StudentProfile;
import com.tricia.smartmentor.repository.MissionRepository;
import com.tricia.smartmentor.repository.StudentProfileRepository;
import com.tricia.smartmentor.util.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EngagementService {

    private final MissionRepository missionRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    private static final String LEADERBOARD_KEY = "leaderboard:exp";

    // Level thresholds: Lv1=0, Lv2=200, Lv3=500, Lv4=1000, Lv5=2000, Lv6=3000, Lv7=5000, Lv8=8000, Lv9=12000, Lv10=20000
    private static final int[] LEVEL_THRESHOLDS = {0, 0, 200, 500, 1000, 2000, 3000, 5000, 8000, 12000, 20000};
    private static final String[] LEVEL_TITLES = {"", "课程学徒", "课程入门者", "课程探索者", "课程实践者",
            "课程进阶者", "课程挑战者", "课程突破者", "课程达人", "课程高手", "课程学霸"};

    // ======================== 获取今日任务 ========================
    @Transactional
    public Map<String, Object> getMissions(Long studentId, String dateStr) {
        LocalDate date = dateStr != null && !dateStr.isBlank()
                ? LocalDate.parse(dateStr) : LocalDate.now();

        List<Mission> missions = missionRepository.findByStudentIdAndMissionDate(studentId, date);

        // Auto-create missions if none exist for today
        if (missions.isEmpty() && !date.isAfter(LocalDate.now())) {
            missions = createDailyMissions(studentId, date);
        }

        List<Map<String, Object>> missionList = new ArrayList<>();
        int completedCount = 0;
        int todayExpEarned = 0;
        int todayExpAvailable = 0;

        for (Mission m : missions) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("missionId", m.getMissionId());
            item.put("title", m.getTitle());
            item.put("description", m.getDescription());
            item.put("type", m.getType());
            item.put("rewardExp", m.getRewardExp());
            item.put("status", m.getStatus());
            item.put("completedAt", m.getCompletedAt() != null ? m.getCompletedAt().toString() : null);
            missionList.add(item);

            if ("completed".equals(m.getStatus())) {
                completedCount++;
                todayExpEarned += m.getRewardExp();
            }
            todayExpAvailable += m.getRewardExp();
        }

        // Get streak info
        StudentProfile profile = studentProfileRepository.findByStudentId(studentId).orElse(null);
        int streakDays = profile != null && profile.getStreakDays() != null ? profile.getStreakDays() : 0;

        // Build streak bonus info
        Map<String, Object> streakBonus = new LinkedHashMap<>();
        streakBonus.put("currentStreak", streakDays);
        int nextMilestone = getNextStreakMilestone(streakDays);
        streakBonus.put("nextMilestone", nextMilestone);
        streakBonus.put("nextMilestoneReward", getStreakMilestoneReward(nextMilestone));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", date.toString());
        result.put("streakDays", streakDays);
        result.put("todayExpEarned", todayExpEarned);
        result.put("todayExpAvailable", todayExpAvailable);
        result.put("missions", missionList);
        result.put("streakBonus", streakBonus);
        return result;
    }

    // ======================== 完成任务 ========================
    @Transactional
    public Map<String, Object> completeMission(Long studentId, String missionId) {
        Mission mission = missionRepository.findByMissionId(missionId)
                .orElseThrow(() -> new BusinessException(404, "任务不存在"));

        if (!mission.getStudentId().equals(studentId)) {
            throw new BusinessException(403, "无权操作此任务");
        }

        if ("completed".equals(mission.getStatus())) {
            throw new BusinessException(400, "任务已完成，不可重复提交");
        }

        // Mark mission completed
        mission.setStatus("completed");
        mission.setCompletedAt(LocalDateTime.now());
        missionRepository.save(mission);

        // Award exp
        int rewardExp = mission.getRewardExp();
        StudentProfile profile = studentProfileRepository.findByStudentId(studentId).orElse(null);

        if (profile == null) {
            profile = new StudentProfile();
            profile.setStudentId(studentId);
            profile.setLevel(1);
            profile.setExperiencePoints(0);
            profile.setStreakDays(0);
        }

        int previousLevel = profile.getLevel() != null ? profile.getLevel() : 1;
        int previousExp = profile.getExperiencePoints() != null ? profile.getExperiencePoints() : 0;

        // Check streak (update if first completion today)
        LocalDate today = LocalDate.now();
        if (profile.getUpdatedAt() != null) {
            LocalDate lastActive = profile.getUpdatedAt().toLocalDate();
            if (lastActive.isBefore(today)) {
                if (lastActive.equals(today.minusDays(1))) {
                    profile.setStreakDays((profile.getStreakDays() != null ? profile.getStreakDays() : 0) + 1);
                } else if (!lastActive.equals(today)) {
                    profile.setStreakDays(1);
                }
            }
        } else {
            profile.setStreakDays(1);
        }

        int streakDays = profile.getStreakDays() != null ? profile.getStreakDays() : 0;

        // Check achievements & bonus exp
        List<Map<String, Object>> newAchievements = new ArrayList<>();
        int bonusExp = 0;

        if (streakDays == 3) {
            bonusExp += 20;
            newAchievements.add(buildAchievement("ach_streak_3", "三天坚持", "连续学习3天", "streak_3", 20));
        } else if (streakDays == 7) {
            bonusExp += 50;
            newAchievements.add(buildAchievement("ach_streak_7", "一周坚持", "连续学习7天", "streak_7", 50));
        } else if (streakDays == 14) {
            bonusExp += 100;
            newAchievements.add(buildAchievement("ach_streak_14", "两周坚持", "连续学习14天", "streak_14", 100));
        } else if (streakDays == 30) {
            bonusExp += 200;
            newAchievements.add(buildAchievement("ach_streak_30", "月度学霸", "连续学习30天", "streak_30", 200));
        }

        int totalEarned = rewardExp + bonusExp;
        int currentExp = previousExp + totalEarned;
        profile.setExperiencePoints(currentExp);

        // Check level up
        int currentLevel = calculateLevel(currentExp);
        boolean leveledUp = currentLevel > previousLevel;
        profile.setLevel(currentLevel);

        if (leveledUp) {
            newAchievements.add(buildAchievement("ach_level_" + currentLevel, "升级到Lv" + currentLevel,
                    "恭喜晋级" + LEVEL_TITLES[currentLevel] + "！", "level_up", 30));
        }

        studentProfileRepository.save(profile);

        // Update Redis leaderboard
        try {
            redisUtil.zIncrBy(LEADERBOARD_KEY, String.valueOf(studentId), totalEarned);
        } catch (Exception ignored) { }

        // Calculate today's progress
        List<Mission> todayMissions = missionRepository.findByStudentIdAndMissionDate(studentId, today);
        int todayCompleted = 0;
        int todayTotalExp = 0;
        for (Mission m : todayMissions) {
            if ("completed".equals(m.getStatus())) {
                todayCompleted++;
                todayTotalExp += m.getRewardExp();
            }
        }

        // Build response matching API doc format
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("missionId", missionId);
        result.put("status", "completed");
        result.put("completedAt", mission.getCompletedAt().toString());
        result.put("rewardExp", rewardExp);

        Map<String, Object> expSummary = new LinkedHashMap<>();
        expSummary.put("previousExp", previousExp);
        expSummary.put("earnedExp", rewardExp);
        if (bonusExp > 0) {
            expSummary.put("bonusExp", bonusExp);
        }
        expSummary.put("currentExp", currentExp);
        expSummary.put("nextLevelExp", currentLevel < 10 ? LEVEL_THRESHOLDS[currentLevel + 1] : LEVEL_THRESHOLDS[10]);
        result.put("expSummary", expSummary);

        result.put("levelUp", leveledUp);
        if (leveledUp) {
            result.put("previousLevel", previousLevel);
        }
        result.put("currentLevel", currentLevel);
        result.put("currentLevelTitle", LEVEL_TITLES[currentLevel]);
        result.put("streakDays", streakDays);
        result.put("newAchievements", newAchievements);

        Map<String, Object> todayProgress = new LinkedHashMap<>();
        todayProgress.put("completedMissions", todayCompleted);
        todayProgress.put("totalMissions", todayMissions.size());
        todayProgress.put("todayTotalExp", todayTotalExp);
        result.put("todayProgress", todayProgress);

        return result;
    }

    // ======================== helpers ========================

    private List<Mission> createDailyMissions(Long studentId, LocalDate date) {
        String datePrefix = date.toString().replace("-", "");
        List<Mission> missions = new ArrayList<>();

        // 读画像，按薄弱模块/学习目标/常见错误/课程个性化任务文案
        StudentProfile profile = studentProfileRepository.findByStudentId(studentId).orElse(null);
        String currentCourse = profile != null ? profile.getCurrentCourse() : null;
        String learningGoal = profile != null ? profile.getLearningGoal() : null;
        List<String> weakModules = parseStringList(profile != null ? profile.getWeakModulePriority() : null);
        String topError = topErrorType(profile != null ? profile.getErrorPatterns() : null);

        // 任务1：诊断（优先锚定薄弱模块，其次当前课程）
        String diagDesc;
        if (!weakModules.isEmpty()) {
            diagDesc = "针对你的薄弱模块「" + String.join("、", weakModules.subList(0, Math.min(2, weakModules.size())))
                    + "」完成一次诊断测试（5-8题），精准定位短板";
        } else if (currentCourse != null && !currentCourse.isBlank()) {
            diagDesc = "完成《" + currentCourse + "》的一次诊断测试（5-8题）";
        } else {
            diagDesc = "选择当前课程完成一次诊断测试（5-8题）";
        }
        missions.add(buildMission(studentId, "mission_" + datePrefix + "_diag",
                "完成1组诊断测试", diagDesc, "diagnostic", 50, date));

        // 任务2：学习路径推进（带上学习目标）
        String learnDesc = (learningGoal != null && !learningGoal.isBlank())
                ? "围绕你的目标「" + learningGoal + "」，在学习路径中完成一个节点并通过检查点"
                : "在当前学习路径中完成一个课程节点并通过检查点测试";
        missions.add(buildMission(studentId, "mission_" + datePrefix + "_learn",
                "学习路径推进1个知识点", learnDesc, "learning", 80, date));

        // 任务3：纠错（针对历史高频错误类型）
        String reviewDesc = (topError != null)
                ? "针对你常见的「" + topError + "」类错误做专项强化，巩固易错点"
                : "重做昨天做错的题目，巩固薄弱点";
        missions.add(buildMission(studentId, "mission_" + datePrefix + "_review",
                "巩固薄弱与错题", reviewDesc, "review", 30, date));

        return missions;
    }

    private Mission buildMission(Long studentId, String missionId, String title,
                                 String description, String type, int rewardExp, LocalDate date) {
        Mission m = new Mission();
        m.setMissionId(missionId);
        m.setStudentId(studentId);
        m.setTitle(title);
        m.setDescription(description);
        m.setType(type);
        m.setRewardExp(rewardExp);
        m.setStatus("pending");
        m.setMissionDate(date);
        return missionRepository.save(m);
    }

    /** 解析画像 JSON 数组字段（weakModulePriority 等）为字符串列表，失败返回空列表 */
    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            List<?> raw = objectMapper.readValue(json, List.class);
            List<String> out = new ArrayList<>();
            for (Object o : raw) {
                if (o != null && !String.valueOf(o).isBlank()) out.add(String.valueOf(o));
            }
            return out;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /** 从 errorPatterns JSON（{错误类型: 次数}）取频率最高的错误类型，无则返回 null */
    private String topErrorType(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            Map<?, ?> map = objectMapper.readValue(json, Map.class);
            String best = null;
            double bestCount = -1;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getValue() instanceof Number) {
                    double c = ((Number) e.getValue()).doubleValue();
                    if (c > bestCount) { bestCount = c; best = String.valueOf(e.getKey()); }
                }
            }
            return best;
        } catch (Exception e) {
            return null;
        }
    }

    private int calculateLevel(int exp) {
        for (int lv = 10; lv >= 1; lv--) {
            if (exp >= LEVEL_THRESHOLDS[lv]) {
                return lv;
            }
        }
        return 1;
    }

    private Map<String, Object> buildAchievement(String achievementId, String title, String description,
                                                  String icon, int bonusExp) {
        Map<String, Object> achievement = new LinkedHashMap<>();
        achievement.put("achievementId", achievementId);
        achievement.put("title", title);
        achievement.put("description", description);
        achievement.put("icon", icon);
        achievement.put("bonusExp", bonusExp);
        return achievement;
    }

    private int getNextStreakMilestone(int currentStreak) {
        int[] milestones = {3, 7, 14, 30, 60, 100};
        for (int m : milestones) {
            if (currentStreak < m) return m;
        }
        return 100;
    }

    private String getStreakMilestoneReward(int milestone) {
        switch (milestone) {
            case 3: return "额外20经验值 + '三天坚持'成就勋章";
            case 7: return "额外50经验值 + '一周坚持'成就勋章";
            case 14: return "额外100经验值 + '两周坚持'成就勋章";
            case 30: return "额外200经验值 + '月度学霸'成就勋章";
            case 60: return "额外500经验值 + '坚持两月'成就勋章";
            case 100: return "额外1000经验值 + '百日精进'成就勋章";
            default: return "额外经验值奖励";
        }
    }
}
