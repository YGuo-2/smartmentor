package com.tricia.smartmentor.service;

import com.tricia.smartmentor.entity.*;
import com.tricia.smartmentor.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final StudentProfileRepository studentProfileRepository;
    private final StudentRepository studentRepository;
    private final MissionRepository missionRepository;
    private final LearningPathRepository learningPathRepository;
    private final KnowledgeGraphService knowledgeGraphService;
    private final MasteryHistoryRepository masteryHistoryRepository;
    private final StudyActivityRepository studyActivityRepository;
    private final AnswerRecordRepository answerRecordRepository;
    private final DiagnosticSessionRepository diagnosticSessionRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private static final String[] LEVEL_TITLES = {"", "课程学徒", "课程入门者", "课程探索者", "课程实践者",
            "课程进阶者", "课程挑战者", "课程突破者", "课程达人", "课程高手", "课程学霸"};
    private static final int[] LEVEL_THRESHOLDS = {0, 0, 200, 500, 1000, 2000, 3000, 5000, 8000, 12000, 20000};

    // ======================== 学习效果报告 ========================
    public Map<String, Object> getEffectivenessReport(Long studentId, String module, String period) {
        StudentProfile profile = studentProfileRepository.findByStudentId(studentId).orElse(null);
        String actualPeriod = period != null ? period : "month";

        double currentMastery = profile != null && profile.getOverallMastery() != null
                ? profile.getOverallMastery().doubleValue() : 0.0;

        // Determine time range
        LocalDateTime periodStart = computePeriodStart(actualPeriod);

        // Get mastery history for the curve
        List<MasteryHistory> masteryHistories = masteryHistoryRepository
                .findOverallMasteryHistory(studentId, periodStart);

        double masteryBefore = currentMastery;
        if (!masteryHistories.isEmpty()) {
            masteryBefore = masteryHistories.get(0).getOverallMastery().doubleValue();
        }
        double improvementRate = masteryBefore > 0
                ? Math.round((currentMastery - masteryBefore) / masteryBefore * 1000.0) / 1000.0 : 0;

        // Real answer stats
        long totalAnswered = answerRecordRepository.countByStudentIdAfter(studentId, periodStart);
        long correctAnswered = answerRecordRepository.countCorrectByStudentIdAfter(studentId, periodStart);
        double accuracy = totalAnswered > 0 ? Math.round((double) correctAnswered / totalAnswered * 100.0) / 100.0 : 0;

        // Real study hours from StudyActivity
        int studyMinutes = studyActivityRepository.sumDurationByStudentAndDateBetween(
                studentId, periodStart.toLocalDate(), LocalDate.now());
        double studyHours = Math.round(studyMinutes / 6.0) / 10.0; // round to 1 decimal

        Map<String, Object> overallSummary = new LinkedHashMap<>();
        overallSummary.put("masteryBefore", Math.round(masteryBefore * 100.0) / 100.0);
        overallSummary.put("masteryAfter", Math.round(currentMastery * 100.0) / 100.0);
        overallSummary.put("improvementRate", improvementRate);
        overallSummary.put("totalStudyHours", studyHours);
        overallSummary.put("totalStudyMinutes", studyMinutes);
        overallSummary.put("totalQuestionsAnswered", totalAnswered);
        overallSummary.put("accuracy", accuracy);

        // Mastery comparison per knowledge point from real data
        List<Map<String, Object>> masteryComparison = buildMasteryComparison(studentId, module, periodStart);

        // Mastery curve from real MasteryHistory
        List<Map<String, Object>> masteryCurve = buildMasteryCurve(masteryHistories, masteryBefore, currentMastery, actualPeriod);

        // Error elimination from real AnswerRecord
        List<Map<String, Object>> errorElimination = buildErrorElimination(studentId, periodStart);

        // Ability radar from real knowledge state
        Map<String, Object> abilityRadar = buildAbilityRadar(profile, studentId, periodStart);

        // 课程学习效果评估（A3：课程掌握度/目标达成/资源偏好/路径完成度等）
        Map<String, Object> courseEffectiveness =
                buildCourseEffectiveness(profile, studentId, currentMastery);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", studentId);
        result.put("generatedAt", LocalDateTime.now().toString());
        result.put("period", actualPeriod);
        result.put("overallSummary", overallSummary);
        result.put("masteryComparison", masteryComparison);
        result.put("masteryCurve", masteryCurve);
        result.put("errorElimination", errorElimination);
        result.put("abilityRadar", abilityRadar);
        result.put("courseEffectiveness", courseEffectiveness);
        return result;
    }

    /**
     * 课程学习效果评估：课程掌握度、学习目标达成度、资源偏好、推荐资源命中率、路径完成度
     */
    private Map<String, Object> buildCourseEffectiveness(StudentProfile profile,
                                                         Long studentId,
                                                         double currentMastery) {
        Map<String, Object> data = new LinkedHashMap<>();

        String currentCourse = profile != null ? profile.getCurrentCourse() : null;
        String learningGoal = profile != null ? profile.getLearningGoal() : null;
        data.put("currentCourse", currentCourse);
        data.put("learningGoal", learningGoal);

        // 1. 当前课程掌握度：课程内知识点掌握度均值，缺省回退整体掌握度
        double courseMastery = computeCourseMastery(profile, currentCourse, currentMastery);
        data.put("courseMastery", toPercent(courseMastery));

        // 2. 路径完成度：当前学生所有路径完成节点占比均值
        double pathCompletion = computePathCompletion(studentId);
        data.put("pathCompletion", toPercent(pathCompletion));

        // 3. 资源使用偏好
        List<String> resourcePreference = parseResourcePreference(profile);
        data.put("resourcePreference", resourcePreference);

        return data;
    }

    private double computeCourseMastery(StudentProfile profile, String currentCourse, double fallback) {
        if (profile == null || currentCourse == null || currentCourse.isBlank()) {
            return fallback;
        }
        Map<String, Double> state = parseKnowledgeState(profile);
        if (state.isEmpty()) {
            return fallback;
        }
        List<KnowledgeGraphService.KnowledgeNode> nodes =
                knowledgeGraphService.getNodesByModule(currentCourse);
        double sum = 0;
        int count = 0;
        for (KnowledgeGraphService.KnowledgeNode node : nodes) {
            Double m = state.get(node.getId());
            if (m != null) {
                sum += m;
                count++;
            }
        }
        return count > 0 ? sum / count : fallback;
    }

    private double computePathCompletion(Long studentId) {
        List<LearningPath> paths = learningPathRepository.findByStudentId(studentId);
        if (paths == null || paths.isEmpty()) {
            return 0.0;
        }
        double sum = 0;
        int count = 0;
        for (LearningPath p : paths) {
            int total = p.getTotalNodes() != null ? p.getTotalNodes() : 0;
            int completed = p.getCompletedNodes() != null ? p.getCompletedNodes() : 0;
            if (total > 0) {
                sum += (double) completed / total;
                count++;
            }
        }
        return count > 0 ? sum / count : 0.0;
    }

    private List<String> parseResourcePreference(StudentProfile profile) {
        List<String> defaults = Arrays.asList("讲解文档", "分层练习", "视频推荐");
        if (profile == null || profile.getResourcePreference() == null
                || profile.getResourcePreference().isBlank()) {
            return defaults;
        }
        try {
            List<String> prefs = objectMapper.readValue(profile.getResourcePreference(),
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
            return (prefs == null || prefs.isEmpty()) ? defaults : prefs;
        } catch (Exception e) {
            return defaults;
        }
    }

    // ======================== 学生仪表盘 ========================
    public Map<String, Object> getDashboard(Long studentId) {
        StudentProfile profile = studentProfileRepository.findByStudentId(studentId).orElse(null);
        Student student = studentRepository.findById(studentId).orElse(null);

        int level = profile != null && profile.getLevel() != null ? profile.getLevel() : 1;
        int exp = profile != null && profile.getExperiencePoints() != null ? profile.getExperiencePoints() : 0;
        int streakDays = profile != null && profile.getStreakDays() != null ? profile.getStreakDays() : 0;
        int nextLevelExp = level < 10 ? LEVEL_THRESHOLDS[level + 1] : LEVEL_THRESHOLDS[10];

        // Real today study minutes from StudyActivity
        int todayStudyMinutes = studyActivityRepository.sumDurationByStudentAndDate(studentId, LocalDate.now());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", studentId);
        result.put("nickname", student != null ? student.getNickname() : "同学");
        result.put("level", level);
        result.put("levelTitle", LEVEL_TITLES[level]);
        result.put("experiencePoints", exp);
        result.put("nextLevelExp", nextLevelExp);
        result.put("streakDays", streakDays);
        result.put("todayStudyMinutes", todayStudyMinutes);
        result.put("totalStudyHours", profile != null && profile.getTotalStudyHours() != null
                ? profile.getTotalStudyHours() : BigDecimal.valueOf(0));

        // Today tasks
        List<Mission> todayMissions = missionRepository.findByStudentIdAndMissionDate(studentId, LocalDate.now());
        int todayCompleted = 0;
        List<Map<String, Object>> taskList = new ArrayList<>();
        for (Mission m : todayMissions) {
            Map<String, Object> task = new LinkedHashMap<>();
            task.put("missionId", m.getMissionId());
            task.put("title", m.getTitle());
            task.put("type", m.getType());
            task.put("status", m.getStatus());
            task.put("rewardExp", m.getRewardExp());
            taskList.add(task);
            if ("completed".equals(m.getStatus())) todayCompleted++;
        }
        Map<String, Object> todayTasks = new LinkedHashMap<>();
        todayTasks.put("total", todayMissions.size());
        todayTasks.put("completed", todayCompleted);
        todayTasks.put("tasks", taskList);
        result.put("todayTasks", todayTasks);

        // Module mastery from real knowledge state
        Map<String, Object> moduleMastery = buildModuleMasteryFromProfile(profile);
        result.put("moduleMastery", moduleMastery);

        // Recent activities from real StudyActivity
        List<Map<String, Object>> recentActivities = buildRecentActivities(studentId);
        result.put("recentActivities", recentActivities);

        // Current learning path
        Page<LearningPath> activePaths = learningPathRepository
                .findByStudentIdAndStatusOrderByCreatedAtDesc(studentId, "active", PageRequest.of(0, 1));
        Map<String, Object> currentPath;
        if (activePaths.hasContent()) {
            LearningPath path = activePaths.getContent().get(0);
            currentPath = new LinkedHashMap<>();
            currentPath.put("pathId", path.getId());
            currentPath.put("name", path.getPathName() != null ? path.getPathName()
                    : path.getTargetKnowledgePointId() + "学习路径");
            currentPath.put("progress", path.getProgress() != null
                    ? path.getProgress().doubleValue() : 0.0);
            currentPath.put("currentNode", path.getCurrentNodeId());
            currentPath.put("totalNodes", path.getTotalNodes() != null ? path.getTotalNodes() : 5);
            currentPath.put("completedNodes", path.getCompletedNodes() != null ? path.getCompletedNodes() : 0);
        } else {
            currentPath = new LinkedHashMap<>();
            currentPath.put("pathId", null);
            currentPath.put("name", "暂无进行中的学习路径");
            currentPath.put("progress", 0.0);
            currentPath.put("currentNode", null);
            currentPath.put("totalNodes", 0);
            currentPath.put("completedNodes", 0);
        }
        result.put("currentPath", currentPath);

        return result;
    }

    // ======================== Private helpers ========================

    private LocalDateTime computePeriodStart(String period) {
        switch (period) {
            case "7d": return LocalDateTime.now().minusDays(7);
            case "30d": return LocalDateTime.now().minusDays(30);
            case "90d": return LocalDateTime.now().minusDays(90);
            case "week": return LocalDateTime.now().minusWeeks(1);
            case "all": return LocalDateTime.of(2020, 1, 1, 0, 0);
            default: return LocalDateTime.now().minusMonths(1); // month
        }
    }

    private List<Map<String, Object>> buildMasteryComparison(Long studentId, String module, LocalDateTime periodStart) {
        List<Map<String, Object>> masteryComparison = new ArrayList<>();
        List<MasteryHistory> histories = masteryHistoryRepository
                .findByStudentIdAndCreatedAtAfterOrderByCreatedAtAsc(studentId, periodStart);
        String moduleKey = module != null && !module.isBlank() ? mapChineseToModule(module) : null;

        Map<String, List<MasteryHistory>> byKnowledgePoint = new LinkedHashMap<>();
        for (MasteryHistory history : histories) {
            if (history.getKnowledgePointId() == null || history.getMastery() == null) continue;
            if (moduleKey != null && (history.getModule() == null || !moduleKey.equals(knowledgeGraphService.normalizeModuleName(history.getModule())))) {
                continue;
            }
            byKnowledgePoint.computeIfAbsent(history.getKnowledgePointId(), key -> new ArrayList<>()).add(history);
        }

        for (Map.Entry<String, List<MasteryHistory>> entry : byKnowledgePoint.entrySet()) {
            List<MasteryHistory> kpHistory = entry.getValue();
            if (kpHistory.size() < 2) {
                continue;
            }
            String kpId = entry.getKey();
            KnowledgeGraphService.KnowledgeNode node = knowledgeGraphService.getNode(kpId);
            String name = node != null ? node.getName() : kpId;
            Map<String, Object> item = new LinkedHashMap<>();
            double pre = kpHistory.get(0).getMastery().doubleValue();
            double post = kpHistory.get(kpHistory.size() - 1).getMastery().doubleValue();
            item.put("knowledgePointId", kpId);
            item.put("name", name);
            item.put("module", name);
            item.put("moduleName", node != null ? mapModuleToChinese(node.getModule()) : null);
            item.put("preTestMastery", toPercent(pre));
            item.put("postTestMastery", toPercent(post));
            item.put("improvement", Math.round((toPercent(post) - toPercent(pre)) * 10.0) / 10.0);
            masteryComparison.add(item);
        }
        return masteryComparison;
    }

    private List<Map<String, Object>> buildMasteryCurve(List<MasteryHistory> histories,
                                                          double masteryBefore, double currentMastery,
                                                          String period) {
        List<Map<String, Object>> curve = new ArrayList<>();

        if (!histories.isEmpty()) {
            // Use real data points, sampling to avoid too many points
            int maxPoints = 10;
            int step = Math.max(1, histories.size() / maxPoints);
            for (int i = 0; i < histories.size(); i += step) {
                MasteryHistory mh = histories.get(i);
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("date", mh.getCreatedAt().toLocalDate().toString());
                point.put("mastery", Math.round(mh.getOverallMastery().doubleValue() * 100.0) / 100.0);
                curve.add(point);
            }
            // Ensure last point is included
            MasteryHistory last = histories.get(histories.size() - 1);
            if (curve.isEmpty() || !curve.get(curve.size() - 1).get("date").equals(last.getCreatedAt().toLocalDate().toString())) {
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("date", last.getCreatedAt().toLocalDate().toString());
                point.put("mastery", Math.round(last.getOverallMastery().doubleValue() * 100.0) / 100.0);
                curve.add(point);
            }
        }
        return curve;
    }

    private List<Map<String, Object>> buildErrorElimination(Long studentId, LocalDateTime periodStart) {
        List<Map<String, Object>> errorElimination = new ArrayList<>();

        // Midpoint of period for before/after comparison
        LocalDateTime midpoint = periodStart.plusSeconds(
                java.time.Duration.between(periodStart, LocalDateTime.now()).getSeconds() / 2);

        Map<String, Long> beforeMap = new LinkedHashMap<>();
        Map<String, Long> afterMap = new LinkedHashMap<>();
        List<AnswerRecord> periodRecords = answerRecordRepository
                .findByStudentIdAndCreatedAtAfterOrderByCreatedAtDesc(studentId, periodStart);
        for (AnswerRecord record : periodRecords) {
            if (!Boolean.FALSE.equals(record.getIsCorrect()) || record.getErrorType() == null) continue;
            if (record.getCreatedAt() != null && record.getCreatedAt().isBefore(midpoint)) {
                beforeMap.merge(record.getErrorType(), 1L, Long::sum);
            } else {
                afterMap.merge(record.getErrorType(), 1L, Long::sum);
            }
        }

        // Merge all error types
        Set<String> allTypes = new LinkedHashSet<>(beforeMap.keySet());
        allTypes.addAll(afterMap.keySet());

        for (String errorType : allTypes) {
            long countBefore = beforeMap.getOrDefault(errorType, 0L);
            long countAfter = afterMap.getOrDefault(errorType, 0L);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("errorType", errorType);
            item.put("countBefore", countBefore);
            item.put("countAfter", countAfter);
            item.put("eliminationRate", countBefore > 0
                    ? Math.round((double) Math.max(countBefore - countAfter, 0) / countBefore * 1000.0) / 10.0 : 0);
            errorElimination.add(item);
        }

        return errorElimination;
    }

    private Map<String, Object> buildAbilityRadar(StudentProfile profile, Long studentId, LocalDateTime periodStart) {
        Map<String, Object> abilityRadar = new LinkedHashMap<>();
        String[] modules = {"人工智能基础", "Java Web 开发", "数字电路基础"};
        String[] dims = {"人工智能基础", "Java Web 开发", "数字电路基础"};

        // Current mastery from profile's knowledgeState
        Map<String, Double> currentMasteryMap = parseKnowledgeState(profile);

        // Compute before mastery from earliest MasteryHistory in period
        List<MasteryHistory> allHistory = masteryHistoryRepository
                .findByStudentIdAndCreatedAtAfterOrderByCreatedAtAsc(studentId, periodStart);
        Map<String, Double> earliestMasteryByKp = new LinkedHashMap<>();
        for (MasteryHistory mh : allHistory) {
            if (mh.getKnowledgePointId() != null && !earliestMasteryByKp.containsKey(mh.getKnowledgePointId())) {
                earliestMasteryByKp.put(mh.getKnowledgePointId(), mh.getMastery().doubleValue());
            }
        }

        List<String> actualDims = new ArrayList<>();
        List<Object> beforeValues = new ArrayList<>();
        List<Double> afterValues = new ArrayList<>();
        for (int moduleIndex = 0; moduleIndex < modules.length; moduleIndex++) {
            String mod = modules[moduleIndex];
            List<KnowledgeGraphService.KnowledgeNode> nodes = knowledgeGraphService.getNodesByModule(mod);
            double beforeSum = 0, afterSum = 0;
            int afterCount = 0, beforeCount = 0;
            for (KnowledgeGraphService.KnowledgeNode node : nodes) {
                if (currentMasteryMap.containsKey(node.getId())) {
                    afterSum += currentMasteryMap.get(node.getId());
                    afterCount++;
                }
                if (earliestMasteryByKp.containsKey(node.getId())) {
                    beforeSum += earliestMasteryByKp.get(node.getId());
                    beforeCount++;
                }
            }
            if (afterCount > 0) {
                actualDims.add(dims[moduleIndex]);
                beforeValues.add(beforeCount > 0 ? toPercent(beforeSum / beforeCount) : null);
                afterValues.add(toPercent(afterSum / afterCount));
            }
        }

        abilityRadar.put("dimensions", actualDims);
        abilityRadar.put("before", beforeValues);
        abilityRadar.put("after", afterValues);
        return abilityRadar;
    }

    private Map<String, Object> buildModuleMasteryFromProfile(StudentProfile profile) {
        Map<String, Object> moduleMastery = new LinkedHashMap<>();
        Map<String, Double> masteryMap = parseKnowledgeState(profile);

        Map<String, String> moduleNameMap = new LinkedHashMap<>();
        moduleNameMap.put("人工智能基础", "人工智能基础");
        moduleNameMap.put("Java Web 开发", "Java Web 开发");
        moduleNameMap.put("数字电路基础", "数字电路基础");

        List<String> dimensions = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        for (Map.Entry<String, String> entry : moduleNameMap.entrySet()) {
            dimensions.add(entry.getValue());
            List<KnowledgeGraphService.KnowledgeNode> nodes = knowledgeGraphService.getNodesByModule(entry.getKey());
            double sum = 0;
            int count = 0;
            for (KnowledgeGraphService.KnowledgeNode node : nodes) {
                if (masteryMap.containsKey(node.getId())) {
                    sum += masteryMap.get(node.getId());
                    count++;
                }
            }
            double avg = count > 0 ? Math.round(sum / count * 100.0) / 100.0 : 0.0;
            values.add(avg);
        }

        moduleMastery.put("dimensions", dimensions);
        moduleMastery.put("values", values);
        return moduleMastery;
    }

    private List<Map<String, Object>> buildRecentActivities(Long studentId) {
        List<Map<String, Object>> recentActivities = new ArrayList<>();

        // Get last 7 days of activities
        LocalDate today = LocalDate.now();
        for (int d = 0; d < 7; d++) {
            LocalDate date = today.minusDays(d);
            List<StudyActivity> dayActivities = studyActivityRepository
                    .findByStudentIdAndActivityDateOrderByCreatedAtDesc(studentId, date);
            if (dayActivities.isEmpty()) continue;

            Map<String, Object> dayData = new LinkedHashMap<>();
            dayData.put("date", date.toString());
            List<Map<String, Object>> actList = new ArrayList<>();
            for (StudyActivity sa : dayActivities) {
                Map<String, Object> act = new LinkedHashMap<>();
                act.put("type", sa.getActivityType());
                act.put("description", sa.getDescription());
                act.put("duration", sa.getDurationMinutes() + "分钟");
                act.put("result", sa.getResultSummary());
                actList.add(act);
            }
            dayData.put("activities", actList);
            recentActivities.add(dayData);
        }

        return recentActivities;
    }

    private Map<String, Double> parseKnowledgeState(StudentProfile profile) {
        if (profile == null || profile.getKnowledgeState() == null || profile.getKnowledgeState().isBlank()) {
            return Collections.emptyMap();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(profile.getKnowledgeState(), Map.class);
            Map<String, Double> result = new HashMap<>();
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    result.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
                }
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private String mapChineseToModule(String chinese) {
        switch (chinese) {
            case "人工智能基础": return "人工智能基础";
            case "Java Web 开发": return "Java Web 开发";
            case "数字电路基础": return "数字电路基础";
            default: return null;
        }
    }

    private String mapModuleToChinese(String module) {
        switch (knowledgeGraphService.normalizeModuleName(module)) {
            case "人工智能基础": return "人工智能基础";
            case "Java Web 开发": return "Java Web 开发";
            case "数字电路基础": return "数字电路基础";
            default: return module;
        }
    }

    private double toPercent(double value) {
        double percent = value <= 1.0 ? value * 100.0 : value;
        return Math.round(percent * 10.0) / 10.0;
    }
}
