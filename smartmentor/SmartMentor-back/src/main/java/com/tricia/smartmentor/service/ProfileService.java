package com.tricia.smartmentor.service;

import com.tricia.smartmentor.common.BusinessException;
import com.tricia.smartmentor.dto.ProfileSettingsRequest;
import com.tricia.smartmentor.entity.AnswerRecord;
import com.tricia.smartmentor.entity.Student;
import com.tricia.smartmentor.entity.StudentProfile;
import com.tricia.smartmentor.entity.StudyActivity;
import com.tricia.smartmentor.repository.AnswerRecordRepository;
import com.tricia.smartmentor.repository.StudentProfileRepository;
import com.tricia.smartmentor.repository.StudentRepository;
import com.tricia.smartmentor.repository.StudyActivityRepository;
import com.tricia.smartmentor.util.RedisUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ProfileService {

    private final StudentProfileRepository studentProfileRepository;
    private final StudentRepository studentRepository;
    private final RedisUtil redisUtil;
    private final KnowledgeGraphService knowledgeGraphService;
    private final AnswerRecordRepository answerRecordRepository;
    private final StudyActivityRepository studyActivityRepository;

    private static final String PROFILE_CACHE_PREFIX = "profile:overview:";
    private static final int PROFILE_CACHE_MINUTES = 5;
    // ObjectMapper 线程安全，复用单例避免重复构造
    private static final com.fasterxml.jackson.databind.ObjectMapper OBJECT_MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    public ProfileService(StudentProfileRepository studentProfileRepository,
                          StudentRepository studentRepository,
                          RedisUtil redisUtil,
                          KnowledgeGraphService knowledgeGraphService,
                          AnswerRecordRepository answerRecordRepository,
                          StudyActivityRepository studyActivityRepository) {
        this.studentProfileRepository = studentProfileRepository;
        this.studentRepository = studentRepository;
        this.redisUtil = redisUtil;
        this.knowledgeGraphService = knowledgeGraphService;
        this.answerRecordRepository = answerRecordRepository;
        this.studyActivityRepository = studyActivityRepository;
    }

    // ======================== 获取五维画像总览 ========================

    public Map<String, Object> getProfileOverview(Long studentId) {
        // Try Redis cache first
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> cached = (Map<String, Object>) redisUtil.get(PROFILE_CACHE_PREFIX + studentId);
            if (cached != null) return cached;
        } catch (Exception ignored) { }

        StudentProfile profile = studentProfileRepository.findByStudentId(studentId)
                .orElseThrow(() -> new BusinessException(404, "学生画像数据不存在，请先完成诊断测试"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", studentId);
        result.put("overallMastery", profile.getOverallMastery());
        result.put("level", profile.getLevel());
        result.put("experiencePoints", profile.getExperiencePoints());
        result.put("streakDays", profile.getStreakDays());
        result.put("totalStudyHours", profile.getTotalStudyHours());

        Map<String, Object> dimensions = new LinkedHashMap<>();

        // Dimension 1: Knowledge State
        dimensions.put("knowledgeState", buildKnowledgeStateDimension(profile));

        // Dimension 2: Error Patterns
        dimensions.put("errorPatterns", buildErrorPatternsDimension(profile));

        // Dimension 3: Learning Behavior
        dimensions.put("learningBehavior", buildLearningBehaviorDimension(profile));

        // Dimension 4: Cognitive Style
        dimensions.put("cognitiveStyle", buildCognitiveStyleDimension(profile));

        // Dimension 5: Goal Profile
        dimensions.put("goalProfile", buildGoalProfileDimension(profile));

        // Dimension 6: Resource Preference & Learning Pace（资源偏好与学习节奏）
        dimensions.put("resourcePreference", buildResourcePreferenceDimension(profile));

        result.put("dimensions", dimensions);
        result.put("overallProfile", buildOverallProfile(profile));
        result.put("subjectProfiles", buildSubjectProfiles(profile));
        result.put("lastUpdatedAt", profile.getUpdatedAt() != null
                ? profile.getUpdatedAt().toString() : null);

        // Cache in Redis
        try {
            redisUtil.set(PROFILE_CACHE_PREFIX + studentId, result,
                    PROFILE_CACHE_MINUTES, java.util.concurrent.TimeUnit.MINUTES);
        } catch (Exception ignored) { }

        return result;
    }

    // ======================== 更新学生设置 ========================

    @Transactional
    public Map<String, Object> updateSettings(Long studentId, ProfileSettingsRequest request) {
        StudentProfile profile = studentProfileRepository.findByStudentId(studentId)
                .orElseThrow(() -> new BusinessException(404, "学生画像数据不存在"));

        if (request.getTargetSchool() != null) {
            profile.setTargetSchool(request.getTargetSchool());
        }
        if (request.getTargetScore() != null) {
            profile.setTargetScore(request.getTargetScore());
        }
        if (request.getMajorDirection() != null) {
            profile.setMajorDirection(request.getMajorDirection());
        }
        if (request.getEducationLevel() != null) {
            profile.setEducationLevel(request.getEducationLevel());
        }
        if (request.getCurrentCourse() != null) {
            profile.setCurrentCourse(request.getCurrentCourse());
        }
        if (request.getLearningGoal() != null) {
            profile.setLearningGoal(request.getLearningGoal());
        }
        if (request.getFoundationLevel() != null) {
            profile.setFoundationLevel(request.getFoundationLevel());
        }
        if (request.getResourcePreference() != null) {
            profile.setResourcePreference(toJson(request.getResourcePreference()));
        }
        if (request.getAcademicInterest() != null) {
            profile.setAcademicInterest(request.getAcademicInterest());
        }
        if (request.getLearningStyle() != null) {
            profile.setLearningStyle(request.getLearningStyle());
        }
        if (request.getDailyStudyMinutes() != null) {
            profile.setDailyStudyMinutes(request.getDailyStudyMinutes());
        }
        if (request.getPreferredTimeSlot() != null) {
            profile.setPreferredTimeSlot(request.getPreferredTimeSlot());
        }
        if (request.getWeakModulePriority() != null) {
            profile.setWeakModulePriority(toJson(request.getWeakModulePriority()));
        }
        if (request.getStudyMode() != null) {
            profile.setStudyMode(request.getStudyMode());
        }

        // Handle nickname/avatar updates on Student entity
        if (request.getNickname() != null || request.getAvatarUrl() != null) {
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new BusinessException(404, "学生不存在"));
            if (request.getNickname() != null) {
                student.setNickname(request.getNickname());
            }
            if (request.getAvatarUrl() != null) {
                student.setAvatarUrl(request.getAvatarUrl());
            }
            studentRepository.save(student);
        }

        studentProfileRepository.save(profile);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", studentId);
        result.put("targetSchool", profile.getTargetSchool());
        result.put("targetScore", profile.getTargetScore());
        result.put("majorDirection", profile.getMajorDirection());
        result.put("educationLevel", profile.getEducationLevel());
        result.put("currentCourse", profile.getCurrentCourse());
        result.put("learningGoal", profile.getLearningGoal());
        result.put("foundationLevel", profile.getFoundationLevel());
        result.put("resourcePreference", parseJsonArray(profile.getResourcePreference()));
        result.put("academicInterest", profile.getAcademicInterest());
        result.put("learningStyle", profile.getLearningStyle());
        result.put("dailyStudyMinutes", profile.getDailyStudyMinutes());
        result.put("preferredTimeSlot", profile.getPreferredTimeSlot());
        result.put("weakModulePriority", parseJsonArray(profile.getWeakModulePriority()));
        result.put("studyMode", profile.getStudyMode());
        result.put("updatedAt", profile.getUpdatedAt() != null
                ? profile.getUpdatedAt().toString() : LocalDateTime.now().toString());

        // Clear profile cache on settings update
        try {
            redisUtil.delete(PROFILE_CACHE_PREFIX + studentId);
        } catch (Exception ignored) { }

        return result;
    }

    // ======================== 对话式画像构建（抽取写入 + 引导判定） ========================

    /** 认知风格合法值（与 buildCognitiveStyleDimension / defaultResourcePrefByStyle 消费的一致） */
    private static final Set<String> VALID_LEARNING_STYLES =
            new HashSet<>(Arrays.asList("visual", "logical", "example", "formula"));

    /** @PrePersist 写入的默认值，用于判定字段是否“仍是默认/未被学生确认” */
    private static final Map<String, String> DEFAULT_VALUES = Map.of(
            "majorDirection", "计算机类",
            "educationLevel", "本科",
            "currentCourse", "人工智能基础",
            "learningGoal", "项目实践",
            "foundationLevel", "基础",
            "learningStyle", "visual");

    /**
     * 把从对话中抽取出的画像特征写入 StudentProfile。
     *
     * @param overwrite true=引导访谈，允许覆盖默认值；
     *                  false=日常对话静默增量，仅在字段为空或仍是出厂默认值时填补，保护学生手填内容
     * @return 实际写入的字段摘要（key -> 新值）；无任何写入时返回空 Map
     */
    @Transactional
    public Map<String, Object> applyExtractedProfile(Long studentId,
                                                     Map<String, Object> extracted,
                                                     boolean overwrite) {
        Map<String, Object> applied = new LinkedHashMap<>();
        if (extracted == null || extracted.isEmpty()) {
            return applied;
        }

        StudentProfile profile = studentProfileRepository.findByStudentId(studentId)
                .orElseGet(() -> {
                    StudentProfile p = new StudentProfile();
                    p.setStudentId(studentId);
                    return p;
                });

        // 字符串型字段
        applyStringField(profile::getMajorDirection, profile::setMajorDirection,
                "majorDirection", extracted, overwrite, applied);
        applyStringField(profile::getEducationLevel, profile::setEducationLevel,
                "educationLevel", extracted, overwrite, applied);
        applyStringField(profile::getCurrentCourse, profile::setCurrentCourse,
                "currentCourse", extracted, overwrite, applied);
        applyStringField(profile::getLearningGoal, profile::setLearningGoal,
                "learningGoal", extracted, overwrite, applied);
        applyStringField(profile::getFoundationLevel, profile::setFoundationLevel,
                "foundationLevel", extracted, overwrite, applied);
        applyStringField(profile::getAcademicInterest, profile::setAcademicInterest,
                "academicInterest", extracted, overwrite, applied);

        // learningStyle 需校验枚举
        String style = stringOf(extracted.get("learningStyle"));
        if (style != null && VALID_LEARNING_STYLES.contains(style)
                && shouldWrite(profile.getLearningStyle(), "learningStyle", overwrite)) {
            profile.setLearningStyle(style);
            applied.put("learningStyle", style);
        }

        // 数组型字段：resourcePreference / weakModulePriority
        applyListField(profile.getResourcePreference(), json -> profile.setResourcePreference(json),
                "resourcePreference", extracted, overwrite, applied);
        applyListField(profile.getWeakModulePriority(), json -> profile.setWeakModulePriority(json),
                "weakModulePriority", extracted, overwrite, applied);

        if (applied.isEmpty()) {
            return applied;
        }

        studentProfileRepository.save(profile);
        try {
            redisUtil.delete(PROFILE_CACHE_PREFIX + studentId);
        } catch (Exception ignored) { }
        return applied;
    }

    @Transactional
    public void appendWeakModulePriority(Long studentId, String moduleOrTopic) {
        String value = stringOf(moduleOrTopic);
        if (value == null) {
            return;
        }
        StudentProfile profile = studentProfileRepository.findByStudentId(studentId)
                .orElseGet(() -> {
                    StudentProfile p = new StudentProfile();
                    p.setStudentId(studentId);
                    return p;
                });

        List<String> weakModules = parseStringList(profile.getWeakModulePriority());
        if (weakModules.stream().noneMatch(item -> item.equalsIgnoreCase(value))) {
            weakModules.add(0, value);
            profile.setWeakModulePriority(toJson(weakModules.subList(0, Math.min(weakModules.size(), 8))));
            studentProfileRepository.save(profile);
            clearProfileCache(studentId);
        }
    }

    @Transactional
    public void appendErrorPattern(Long studentId, String type, String subType) {
        String normalizedType = stringOf(type);
        String normalizedSubType = stringOf(subType);
        if (normalizedType == null && normalizedSubType == null) {
            return;
        }
        if (normalizedType == null) {
            normalizedType = "对话暴露薄弱";
        }
        if (normalizedSubType == null) {
            normalizedSubType = "需进一步诊断";
        }

        StudentProfile profile = studentProfileRepository.findByStudentId(studentId)
                .orElseGet(() -> {
                    StudentProfile p = new StudentProfile();
                    p.setStudentId(studentId);
                    return p;
                });

        List<Map<String, Object>> patterns = parsePatternList(profile.getErrorPatterns());
        for (Map<String, Object> pattern : patterns) {
            if (normalizedType.equals(String.valueOf(pattern.get("type")))
                    && normalizedSubType.equals(String.valueOf(pattern.get("subType")))) {
                Object freq = pattern.get("frequency");
                double current = freq instanceof Number ? ((Number) freq).doubleValue() : 0.1;
                pattern.put("frequency", Math.min(1.0, Math.round((current + 0.1) * 100.0) / 100.0));
                pattern.put("trend", "up");
                profile.setErrorPatterns(toJson(patterns));
                studentProfileRepository.save(profile);
                clearProfileCache(studentId);
                return;
            }
        }

        patterns.add(0, buildPattern(normalizedType, normalizedSubType, 0.2, "up"));
        profile.setErrorPatterns(toJson(patterns.subList(0, Math.min(patterns.size(), 10))));
        studentProfileRepository.save(profile);
        clearProfileCache(studentId);
    }

    /** 判断学生是否需要引导访谈：核心画像字段仍全部为空或停留在出厂默认值 */
    public boolean isOnboardingNeeded(Long studentId) {
        StudentProfile profile = studentProfileRepository.findByStudentId(studentId).orElse(null);
        if (profile == null) {
            return true;
        }
        // 学生若已显式填过任一“非默认”关键字段，或填过资源偏好，则视为已建立画像
        boolean academicSet = profile.getAcademicInterest() != null
                && !profile.getAcademicInterest().isBlank();
        boolean prefSet = profile.getResourcePreference() != null
                && !profile.getResourcePreference().isBlank();
        boolean weakSet = profile.getWeakModulePriority() != null
                && !profile.getWeakModulePriority().isBlank();
        boolean majorCustom = isNonDefault(profile.getMajorDirection(), "majorDirection");
        boolean goalCustom = isNonDefault(profile.getLearningGoal(), "learningGoal");
        boolean courseCustom = isNonDefault(profile.getCurrentCourse(), "currentCourse");
        return !(academicSet || prefSet || weakSet || majorCustom || goalCustom || courseCustom);
    }

    /**
     * 面向 AI 对话的分层画像上下文：总体画像只描述学生的大方向，
     * 科目画像才描述每门课的掌握度和欠缺点，避免对话被 currentCourse 长期锁死。
     */
    public String buildLayeredProfileContext(Long studentId) {
        StudentProfile profile = studentProfileRepository.findByStudentId(studentId).orElse(null);
        if (profile == null) {
            return "【总体画像】\n- 暂未建立画像。\n\n【科目画像】\n- 暂无科目级掌握数据；学生问题未说明科目时，应先确认课程或知识点。\n\n";
        }

        StringBuilder ctx = new StringBuilder();
        ctx.append("【总体画像】\n");
        appendContextLine(ctx, "专业方向", profile.getMajorDirection());
        appendContextLine(ctx, "学历层次", profile.getEducationLevel());
        appendContextLine(ctx, "学习目标", profile.getLearningGoal());
        appendContextLine(ctx, "基础水平", profile.getFoundationLevel());
        appendContextLine(ctx, "兴趣方向", profile.getAcademicInterest());
        appendContextLine(ctx, "学习风格偏好", mapLearningStyleLabel(profile.getLearningStyle()));
        if (profile.getResourcePreference() != null && !profile.getResourcePreference().isBlank()) {
            ctx.append("- 资源偏好：").append(String.join("、", parseStringList(profile.getResourcePreference()))).append("\n");
        }
        if (isNonDefault(profile.getCurrentCourse(), "currentCourse")) {
            ctx.append("- 近期重点课程：").append(profile.getCurrentCourse())
                    .append("（只作为近期线索，不代表所有对话都要聚焦这门课）\n");
        }
        if (profile.getOverallMastery() != null) {
            int pct = profile.getOverallMastery().multiply(java.math.BigDecimal.valueOf(100)).intValue();
            ctx.append("- 跨科目平均掌握度：").append(pct).append("%\n");
        }
        if (profile.getLevel() != null) {
            ctx.append("- 当前等级：Lv.").append(profile.getLevel()).append("\n");
        }
        ctx.append("- 画像使用边界：总体画像用于判断专业方向、目标、讲解深度和资源形式，不等同于某一门课。\n\n");

        ctx.append("【科目画像】\n");
        List<Map<String, Object>> subjects = buildSubjectProfiles(profile);
        if (subjects.isEmpty()) {
            ctx.append("- 暂无科目级掌握数据；学生问题未说明科目时，应先确认课程或知识点。\n");
        } else {
            for (Map<String, Object> subject : subjects) {
                @SuppressWarnings("unchecked")
                List<String> gaps = subject.get("gaps") instanceof List
                        ? (List<String>) subject.get("gaps") : Collections.emptyList();
                ctx.append("- ").append(subject.get("subject"))
                        .append("：掌握度约").append(subject.get("masteryPercent")).append("%")
                        .append("，").append(subject.get("status"))
                        .append("，已评估 ").append(subject.get("observedKnowledgePoints"))
                        .append("/").append(subject.get("totalKnowledgePoints")).append(" 个知识点");
                if (!gaps.isEmpty()) {
                    ctx.append("，欠缺：").append(String.join("、", gaps.subList(0, Math.min(gaps.size(), 4))));
                }
                Object accuracy = subject.get("recentAccuracyPercent");
                if (accuracy != null) {
                    ctx.append("，近期正确率 ").append(accuracy).append("%");
                }
                ctx.append("\n");
            }
        }
        ctx.append("使用规则：学生明确提到哪门课或知识点，就使用对应科目画像；未明确时先追问，不要默认回到近期重点课程。\n\n");
        return ctx.toString();
    }

    // ---- 抽取写入辅助 ----

    private void applyStringField(java.util.function.Supplier<String> getter,
                                  java.util.function.Consumer<String> setter,
                                  String key, Map<String, Object> extracted,
                                  boolean overwrite, Map<String, Object> applied) {
        String val = stringOf(extracted.get(key));
        if (val != null && shouldWrite(getter.get(), key, overwrite)) {
            setter.accept(val);
            applied.put(key, val);
        }
    }

    private void applyListField(String currentJson, java.util.function.Consumer<String> setter,
                                String key, Map<String, Object> extracted,
                                boolean overwrite, Map<String, Object> applied) {
        Object raw = extracted.get(key);
        if (!(raw instanceof List) || ((List<?>) raw).isEmpty()) {
            return;
        }
        boolean currentEmpty = currentJson == null || currentJson.isBlank()
                || "[]".equals(currentJson.trim());
        // 列表字段无“出厂默认”，增量模式仅在当前为空时写
        if (overwrite || currentEmpty) {
            setter.accept(toJson(raw));
            applied.put(key, raw);
        }
    }

    /**
     * 是否应写入：overwrite 模式总写；增量模式仅当当前值为空或仍是出厂默认值时写。
     */
    private boolean shouldWrite(String currentValue, String key, boolean overwrite) {
        if (overwrite) {
            return true;
        }
        if (currentValue == null || currentValue.isBlank()) {
            return true;
        }
        String def = DEFAULT_VALUES.get(key);
        return def != null && def.equals(currentValue);
    }

    private boolean isNonDefault(String value, String key) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String def = DEFAULT_VALUES.get(key);
        return def == null || !def.equals(value);
    }

    private String stringOf(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o).trim();
        return s.isBlank() ? null : s;
    }

    // ======================== 获取知识掌握度图谱 ========================

    public Map<String, Object> getKnowledgeMap(Long studentId, String module, String depthStr) {
        StudentProfile profile = studentProfileRepository.findByStudentId(studentId)
                .orElseThrow(() -> new BusinessException(404, "学生画像数据不存在"));

        int depth = 3;
        if (depthStr != null && !depthStr.isBlank()) {
            depth = Integer.parseInt(depthStr);
            depth = Math.max(1, Math.min(5, depth));
        }

        // Build full knowledge point catalog
        List<Map<String, Object>> allNodes = buildKnowledgePointCatalog(profile);
        List<Map<String, Object>> allEdges = buildKnowledgeEdges();

        Set<String> retainedIds = new LinkedHashSet<>();

        // Filter by module if specified, then keep connected context up to depth hops.
        if (module != null && !module.isBlank()) {
            for (Map<String, Object> n : allNodes) {
                if (module.equals(n.get("module"))) {
                    retainedIds.add(String.valueOf(n.get("id")));
                }
            }

            for (int d = 0; d < depth; d++) {
                Set<String> expanded = new LinkedHashSet<>(retainedIds);
                for (Map<String, Object> e : allEdges) {
                    String source = String.valueOf(e.get("source"));
                    String target = String.valueOf(e.get("target"));
                    if (retainedIds.contains(source) || retainedIds.contains(target)) {
                        expanded.add(source);
                        expanded.add(target);
                    }
                }
                if (expanded.size() == retainedIds.size()) {
                    break;
                }
                retainedIds = expanded;
            }

            List<Map<String, Object>> filteredNodes = new ArrayList<>();
            for (Map<String, Object> n : allNodes) {
                if (retainedIds.contains(String.valueOf(n.get("id")))) {
                    filteredNodes.add(n);
                }
            }
            allNodes = filteredNodes;
        } else {
            for (Map<String, Object> n : allNodes) {
                retainedIds.add(String.valueOf(n.get("id")));
            }
        }

        Set<String> visibleIds = new HashSet<>();
        for (Map<String, Object> n : allNodes) {
            visibleIds.add(String.valueOf(n.get("id")));
        }
        allEdges.removeIf(e -> !visibleIds.contains(String.valueOf(e.get("source")))
                || !visibleIds.contains(String.valueOf(e.get("target"))));

        int masteredCount = 0, learningCount = 0, weakCount = 0;
        for (Map<String, Object> n : allNodes) {
            String level = (String) n.get("masteryLevel");
            if ("mastered".equals(level)) masteredCount++;
            else if ("learning".equals(level)) learningCount++;
            else weakCount++;
        }

        Map<String, Object> legend = new LinkedHashMap<>();
        Map<String, Object> masteryLevels = new LinkedHashMap<>();
        Map<String, Object> masteredDef = new LinkedHashMap<>();
        masteredDef.put("label", "已掌握");
        masteredDef.put("color", "#52c41a");
        masteredDef.put("threshold", "≥ 0.8");
        Map<String, Object> learningDef = new LinkedHashMap<>();
        learningDef.put("label", "学习中");
        learningDef.put("color", "#faad14");
        learningDef.put("threshold", "0.5 - 0.8");
        Map<String, Object> weakDef = new LinkedHashMap<>();
        weakDef.put("label", "薄弱");
        weakDef.put("color", "#ff4d4f");
        weakDef.put("threshold", "< 0.5");
        masteryLevels.put("mastered", masteredDef);
        masteryLevels.put("learning", learningDef);
        masteryLevels.put("weak", weakDef);
        legend.put("masteryLevels", masteryLevels);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", studentId);
        result.put("module", module);
        result.put("depth", depth);
        result.put("totalNodes", allNodes.size());
        result.put("masteredNodes", masteredCount);
        result.put("learningNodes", learningCount);
        result.put("weakNodes", weakCount);
        result.put("nodes", allNodes);
        result.put("edges", allEdges);
        result.put("legend", legend);
        return result;
    }

    // ======================== 分层画像 helpers ========================

    private Map<String, Object> buildOverallProfile(StudentProfile profile) {
        Map<String, Object> overall = new LinkedHashMap<>();
        overall.put("majorDirection", profile.getMajorDirection());
        overall.put("educationLevel", profile.getEducationLevel());
        overall.put("learningGoal", profile.getLearningGoal());
        overall.put("foundationLevel", profile.getFoundationLevel());
        overall.put("academicInterest", profile.getAcademicInterest());
        overall.put("learningStyle", profile.getLearningStyle());
        overall.put("learningStyleLabel", mapLearningStyleLabel(profile.getLearningStyle()));
        overall.put("resourcePreference", parseJsonArray(profile.getResourcePreference()));
        overall.put("studyMode", profile.getStudyMode());
        overall.put("dailyStudyMinutes", profile.getDailyStudyMinutes());
        overall.put("preferredTimeSlot", profile.getPreferredTimeSlot());
        overall.put("overallMastery", profile.getOverallMastery());
        overall.put("recentFocusCourse", isNonDefault(profile.getCurrentCourse(), "currentCourse")
                ? profile.getCurrentCourse() : null);
        overall.put("profileScope", "overall");
        overall.put("scopeDescription", "总体画像描述专业方向、目标、习惯和资源偏好；科目掌握情况见 subjectProfiles。");
        return overall;
    }

    private List<Map<String, Object>> buildSubjectProfiles(StudentProfile profile) {
        Map<String, SubjectProfileAccumulator> subjects = new LinkedHashMap<>();
        Map<String, Double> masteryMap = parseKnowledgeState(profile);

        for (String subject : orderedKnownSubjects()) {
            SubjectProfileAccumulator acc = subjects.computeIfAbsent(subject, SubjectProfileAccumulator::new);
            List<KnowledgeGraphService.KnowledgeNode> nodes = knowledgeGraphService.getNodesByModule(subject);
            for (KnowledgeGraphService.KnowledgeNode node : nodes) {
                acc.totalKnowledgePoints++;
                Double mastery = masteryMap.get(node.getId());
                double normalized = mastery != null ? clamp01(mastery) : 0.3;
                acc.sumMasteryWithDefault += normalized;
                if (mastery != null) {
                    acc.observedKnowledgePoints++;
                    acc.sumObservedMastery += normalized;
                    if (normalized >= 0.8) {
                        acc.masteredCount++;
                    } else if (normalized >= 0.5) {
                        acc.learningCount++;
                    } else {
                        acc.weakCount++;
                    }
                    if (normalized < 0.6) {
                        acc.addGap(node.getName());
                    }
                }
            }
        }

        mergeWeakPriorities(profile, subjects);
        mergeRecentAnswerSignals(profile, subjects);

        List<Map<String, Object>> result = new ArrayList<>();
        for (SubjectProfileAccumulator acc : subjects.values()) {
            result.add(acc.toMap());
        }
        return result;
    }

    private List<String> orderedKnownSubjects() {
        List<String> preferred = Arrays.asList("人工智能基础", "Java Web 开发", "数字电路基础");
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        for (String subject : preferred) {
            if (!knowledgeGraphService.getNodesByModule(subject).isEmpty()) {
                ordered.add(subject);
            }
        }
        List<String> rest = new ArrayList<>();
        for (String module : knowledgeGraphService.getAllModules()) {
            String normalized = knowledgeGraphService.normalizeModuleName(module);
            if (!ordered.contains(normalized)) {
                rest.add(normalized);
            }
        }
        Collections.sort(rest);
        ordered.addAll(rest);
        return new ArrayList<>(ordered);
    }

    private void mergeWeakPriorities(StudentProfile profile,
                                     Map<String, SubjectProfileAccumulator> subjects) {
        for (String item : parseStringList(profile.getWeakModulePriority())) {
            String value = stringOf(item);
            if (value == null) {
                continue;
            }
            String subject = null;
            String gap = value;
            int sep = value.indexOf("·");
            if (sep >= 0) {
                subject = knowledgeGraphService.normalizeModuleName(value.substring(0, sep).trim());
                gap = value.substring(sep + 1).trim();
            } else if (!knowledgeGraphService.getNodesByModule(value).isEmpty()) {
                subject = knowledgeGraphService.normalizeModuleName(value);
                gap = "整体薄弱";
            } else {
                KnowledgeGraphService.KnowledgeNode node = findBestKnowledgeNodeAcrossSubjects(value);
                if (node != null) {
                    subject = knowledgeGraphService.normalizeModuleName(node.getModule());
                    gap = node.getName();
                } else if (isNonDefault(profile.getCurrentCourse(), "currentCourse")) {
                    subject = knowledgeGraphService.normalizeModuleName(profile.getCurrentCourse());
                }
            }
            if (subject != null && !subject.isBlank()) {
                SubjectProfileAccumulator acc = subjects.computeIfAbsent(subject, SubjectProfileAccumulator::new);
                acc.addGap(gap);
                acc.weakSignalCount++;
            }
        }
    }

    private void mergeRecentAnswerSignals(StudentProfile profile,
                                          Map<String, SubjectProfileAccumulator> subjects) {
        List<AnswerRecord> recent = answerRecordRepository
                .findByStudentIdAndCreatedAtAfterOrderByCreatedAtDesc(
                        profile.getStudentId(), LocalDateTime.now().minusDays(60));
        for (AnswerRecord record : recent) {
            String subject = resolveSubjectForAnswer(record, profile);
            if (subject == null || subject.isBlank()) {
                continue;
            }
            SubjectProfileAccumulator acc = subjects.computeIfAbsent(subject, SubjectProfileAccumulator::new);
            acc.answerCount++;
            if (Boolean.TRUE.equals(record.getIsCorrect())) {
                acc.correctCount++;
            } else if (Boolean.FALSE.equals(record.getIsCorrect())) {
                String gap = firstNonBlank(record.getKnowledgePointName(), record.getKnowledgePointId(), record.getErrorType(), "待定位知识点");
                acc.addGap(gap);
                acc.weakSignalCount++;
            }
            if (record.getCreatedAt() != null
                    && (acc.lastActivityAt == null || record.getCreatedAt().isAfter(acc.lastActivityAt))) {
                acc.lastActivityAt = record.getCreatedAt();
            }
        }
    }

    private String resolveSubjectForAnswer(AnswerRecord record, StudentProfile profile) {
        if (record.getKnowledgePointId() != null && !record.getKnowledgePointId().isBlank()) {
            KnowledgeGraphService.KnowledgeNode node = knowledgeGraphService.getNode(record.getKnowledgePointId());
            if (node != null) {
                return knowledgeGraphService.normalizeModuleName(node.getModule());
            }
        }
        KnowledgeGraphService.KnowledgeNode byName = findBestKnowledgeNodeAcrossSubjects(record.getKnowledgePointName());
        if (byName != null) {
            return knowledgeGraphService.normalizeModuleName(byName.getModule());
        }
        return isNonDefault(profile.getCurrentCourse(), "currentCourse")
                ? knowledgeGraphService.normalizeModuleName(profile.getCurrentCourse()) : null;
    }

    private KnowledgeGraphService.KnowledgeNode findBestKnowledgeNodeAcrossSubjects(String knowledgePointName) {
        String target = normalizeForMatch(knowledgePointName);
        if (target.isBlank()) {
            return null;
        }
        KnowledgeGraphService.KnowledgeNode best = null;
        int bestScore = 0;
        for (String subject : orderedKnownSubjects()) {
            for (KnowledgeGraphService.KnowledgeNode node : knowledgeGraphService.getNodesByModule(subject)) {
                int score = matchScore(normalizeForMatch(node.getName()), target);
                if (score > bestScore) {
                    bestScore = score;
                    best = node;
                }
            }
        }
        return bestScore >= 2 ? best : null;
    }

    private int matchScore(String nodeName, String target) {
        if (nodeName.isBlank() || target.isBlank()) {
            return 0;
        }
        if (nodeName.equals(target)) {
            return 100;
        }
        if (nodeName.contains(target) || target.contains(nodeName)) {
            return Math.min(nodeName.length(), target.length());
        }
        int score = 0;
        for (int i = 0; i < target.length() - 1; i++) {
            String gram = target.substring(i, i + 2);
            if (nodeName.contains(gram)) {
                score += 2;
            }
        }
        return score;
    }

    private String normalizeForMatch(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\p{Punct}，。、“”‘’（）()【】\\[\\]：:；;·-]+", "");
    }

    private double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private void appendContextLine(StringBuilder ctx, String label, String value) {
        if (value != null && !value.isBlank()) {
            ctx.append("- ").append(label).append("：").append(value).append("\n");
        }
    }

    private String mapLearningStyleLabel(String style) {
        if (style == null || style.isBlank()) {
            return null;
        }
        switch (style) {
            case "visual": return "视觉型（偏好图表、动画和结构图）";
            case "logical": return "逻辑型（偏好推导和结构化解释）";
            case "example": return "案例型（偏好例题、案例和变式训练）";
            case "formula": return "公式型（偏好公式推导和模板归纳）";
            case "auditory": return "听觉型（偏好语言讲解）";
            case "kinesthetic": return "实践型（偏好动手练习）";
            case "reading": return "阅读型（偏好文字材料）";
            default: return style;
        }
    }

    private static class SubjectProfileAccumulator {
        private final String subject;
        private int totalKnowledgePoints;
        private int observedKnowledgePoints;
        private int masteredCount;
        private int learningCount;
        private int weakCount;
        private int weakSignalCount;
        private int answerCount;
        private int correctCount;
        private double sumMasteryWithDefault;
        private double sumObservedMastery;
        private LocalDateTime lastActivityAt;
        private final LinkedHashSet<String> gaps = new LinkedHashSet<>();

        private SubjectProfileAccumulator(String subject) {
            this.subject = subject;
        }

        private void addGap(String gap) {
            if (gap == null) {
                return;
            }
            String normalized = gap.trim();
            if (!normalized.isBlank()) {
                gaps.add(normalized);
            }
        }

        private Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            double mastery = totalKnowledgePoints > 0
                    ? sumMasteryWithDefault / totalKnowledgePoints
                    : (observedKnowledgePoints > 0 ? sumObservedMastery / observedKnowledgePoints : 0.0);
            mastery = Math.round(mastery * 100.0) / 100.0;

            map.put("subject", subject);
            map.put("course", subject);
            map.put("scope", "subject");
            map.put("mastery", mastery);
            map.put("masteryPercent", (int) Math.round(mastery * 100));
            map.put("status", statusLabel(mastery));
            map.put("totalKnowledgePoints", totalKnowledgePoints);
            map.put("observedKnowledgePoints", observedKnowledgePoints);
            map.put("unassessedKnowledgePoints", Math.max(0, totalKnowledgePoints - observedKnowledgePoints));
            map.put("masteredCount", masteredCount);
            map.put("learningCount", learningCount);
            map.put("weakCount", weakCount);
            map.put("weakSignalCount", weakSignalCount);
            map.put("gaps", new ArrayList<>(gaps).subList(0, Math.min(gaps.size(), 6)));
            map.put("evidenceLevel", evidenceLabel());
            if (answerCount > 0) {
                map.put("recentAnsweredCount", answerCount);
                map.put("recentAccuracyPercent", (int) Math.round(correctCount * 100.0 / answerCount));
            }
            if (lastActivityAt != null) {
                map.put("lastActivityAt", lastActivityAt.toString());
            }
            map.put("summary", summaryText(mastery));
            return map;
        }

        private String statusLabel(double mastery) {
            if (observedKnowledgePoints == 0 && answerCount == 0 && gaps.isEmpty()) {
                return "待诊断";
            }
            if (mastery >= 0.8) return "掌握较好";
            if (mastery >= 0.6) return "学习中";
            if (mastery >= 0.4) return "需要巩固";
            return "薄弱待补";
        }

        private String evidenceLabel() {
            if (observedKnowledgePoints > 0 || answerCount > 0) {
                return "有学习/诊断证据";
            }
            if (!gaps.isEmpty()) {
                return "来自对话薄弱信号";
            }
            return "待诊断";
        }

        private String summaryText(double mastery) {
            if (observedKnowledgePoints == 0 && answerCount == 0 && gaps.isEmpty()) {
                return "尚未形成该科目的细分画像，建议先做一次诊断。";
            }
            if (!gaps.isEmpty()) {
                return "当前主要欠缺：" + String.join("、", new ArrayList<>(gaps).subList(0, Math.min(gaps.size(), 3)));
            }
            if (mastery >= 0.8) {
                return "基础较稳，可进入综合应用或更高难度任务。";
            }
            return "已有部分掌握记录，建议继续补齐未评估知识点。";
        }
    }

    // ======================== 五维画像构建 helpers ========================

    private Map<String, Object> buildKnowledgeStateDimension(StudentProfile profile) {
        Map<String, Object> dim = new LinkedHashMap<>();
        dim.put("label", "知识状态");
        double overallScore = profile.getOverallMastery() != null
                ? profile.getOverallMastery().doubleValue() : 0.0;
        dim.put("score", overallScore);
        dim.put("description", "基于 BKT 模型的知识点掌握概率");

        // Compute module mastery from real knowledge state
        Map<String, Double> masteryMap = parseKnowledgeState(profile);
        Map<String, List<Double>> moduleScores = new LinkedHashMap<>();

        for (String module : knowledgeGraphService.getAllModules()) {
            String chineseName = mapModuleToChinese(module);
            List<Double> scores = new ArrayList<>();
            for (KnowledgeGraphService.KnowledgeNode node : knowledgeGraphService.getNodesByModule(module)) {
                scores.add(masteryMap.getOrDefault(node.getId(), 0.3));
            }
            moduleScores.put(chineseName, scores);
        }

        Map<String, Object> moduleMastery = new LinkedHashMap<>();
        for (Map.Entry<String, List<Double>> entry : moduleScores.entrySet()) {
            double avg = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.3);
            moduleMastery.put(entry.getKey(), Math.round(avg * 100.0) / 100.0);
        }
        dim.put("moduleMastery", moduleMastery);

        // Compute real counts
        int total = knowledgeGraphService.getNodeCount();
        int mastered = 0, learning = 0, weak = 0;
        for (double m : masteryMap.values()) {
            if (m >= 0.8) mastered++;
            else if (m >= 0.5) learning++;
            else weak++;
        }
        // Nodes not in masteryMap are assumed weak
        weak += (total - masteryMap.size());

        dim.put("totalKnowledgePoints", total);
        dim.put("masteredCount", mastered);
        dim.put("learningCount", learning);
        dim.put("weakCount", weak);
        dim.put("subjectProfiles", buildSubjectProfiles(profile));
        return dim;
    }

    private Map<String, Object> buildErrorPatternsDimension(StudentProfile profile) {
        Map<String, Object> dim = new LinkedHashMap<>();
        dim.put("label", "错误模式");
        dim.put("description", "常犯错误类型的分布与频率");

        Long studentId = profile.getStudentId();

        // Get real error counts from AnswerRecord
        long totalErrors = answerRecordRepository.countIncorrectByStudentId(studentId);
        long totalAnswered = answerRecordRepository.countByStudentId(studentId);
        double recentErrorRate = totalAnswered > 0 ? Math.round((double) totalErrors / totalAnswered * 100.0) / 100.0 : 0;

        // Build patterns from real error type distribution
        List<Object[]> errorsByTypeAndDetail = answerRecordRepository.countErrorsByTypeAndDetail(studentId);
        List<Map<String, Object>> patterns = new ArrayList<>();

        if (!errorsByTypeAndDetail.isEmpty()) {
            for (Object[] row : errorsByTypeAndDetail) {
                String errorType = (String) row[0];
                String errorDetail = row[1] != null ? (String) row[1] : errorType;
                long count = ((Number) row[2]).longValue();
                double frequency = totalErrors > 0 ? Math.round((double) count / totalErrors * 100.0) / 100.0 : 0;

                // Determine trend by comparing recent vs older errors
                LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
                List<Object[]> recentErrors = answerRecordRepository.countErrorsByTypeAfter(studentId, oneWeekAgo);
                List<Object[]> olderErrors = answerRecordRepository.countErrorsByTypeBefore(studentId, oneWeekAgo);

                long recentCount = getErrorCountForType(recentErrors, errorType);
                long olderCount = getErrorCountForType(olderErrors, errorType);
                String trend = recentCount > olderCount ? "increasing" : recentCount < olderCount ? "decreasing" : "stable";

                patterns.add(buildPattern(errorType, errorDetail, frequency, trend));
            }
        }

        // Fallback: use stored errorPatterns JSON if no AnswerRecord data
        if (patterns.isEmpty() && profile.getErrorPatterns() != null && !profile.getErrorPatterns().isBlank()) {
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> stored = OBJECT_MAPPER
                        .readValue(profile.getErrorPatterns(),
                                new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
                if (!stored.isEmpty()) {
                    patterns = stored;
                }
            } catch (Exception ignored) {}
        }

        double score = totalAnswered > 0 ? Math.round((1.0 - recentErrorRate) * 100.0) / 100.0 : 0.5;
        dim.put("score", score);
        dim.put("patterns", patterns);
        dim.put("totalErrors", totalErrors);
        dim.put("recentErrorRate", recentErrorRate);
        return dim;
    }

    private long getErrorCountForType(List<Object[]> errorCounts, String errorType) {
        for (Object[] row : errorCounts) {
            if (errorType.equals(row[0])) {
                return ((Number) row[1]).longValue();
            }
        }
        return 0;
    }

    private Map<String, Object> buildLearningBehaviorDimension(StudentProfile profile) {
        Map<String, Object> dim = new LinkedHashMap<>();
        dim.put("label", "学习行为");
        dim.put("description", "学习时段偏好、专注时长与做题速度");

        Long studentId = profile.getStudentId();

        // Real average question time from AnswerRecord
        double avgQuestionTime = answerRecordRepository.avgTimeSpentByStudentId(studentId);
        avgQuestionTime = Math.round(avgQuestionTime * 10.0) / 10.0;

        // Real weekly active days from StudyActivity (last 4 weeks average)
        LocalDate fourWeeksAgo = LocalDate.now().minusWeeks(4);
        int activeDays = studyActivityRepository.countActiveDaysByStudentBetween(studentId, fourWeeksAgo, LocalDate.now());
        int weeklyActiveDays = Math.round(activeDays / 4.0f);

        // Real average daily study minutes from last 7 days
        LocalDate oneWeekAgo = LocalDate.now().minusDays(7);
        int weekTotalMinutes = studyActivityRepository.sumDurationByStudentAndDateBetween(studentId, oneWeekAgo, LocalDate.now());
        int avgDailyStudyMinutes = activeDays > 0 ? weekTotalMinutes / Math.min(activeDays, 7) :
                (profile.getDailyStudyMinutes() != null ? profile.getDailyStudyMinutes() : 0);

        // Compute avg focus duration from study activities (avg session length)
        List<StudyActivity> recentActivities = studyActivityRepository
                .findByStudentIdAndActivityDateBetweenOrderByCreatedAtDesc(studentId, oneWeekAgo, LocalDate.now());
        int avgFocusDuration = 0;
        if (!recentActivities.isEmpty()) {
            int totalDuration = recentActivities.stream()
                    .filter(a -> a.getDurationMinutes() != null)
                    .mapToInt(StudyActivity::getDurationMinutes)
                    .sum();
            long sessionsWithDuration = recentActivities.stream()
                    .filter(a -> a.getDurationMinutes() != null && a.getDurationMinutes() > 0)
                    .count();
            avgFocusDuration = sessionsWithDuration > 0 ? (int) (totalDuration / sessionsWithDuration) : 0;
        }

        // Compute behavior score based on consistency
        double behaviorScore = Math.min(1.0, weeklyActiveDays / 7.0 * 0.5 + (avgDailyStudyMinutes > 0 ? 0.3 : 0) + (avgFocusDuration >= 20 ? 0.2 : avgFocusDuration / 100.0));
        behaviorScore = Math.round(behaviorScore * 100.0) / 100.0;

        dim.put("score", behaviorScore);
        dim.put("preferredTimeSlot", profile.getPreferredTimeSlot() != null
                ? profile.getPreferredTimeSlot() : "evening");
        dim.put("avgDailyStudyMinutes", avgDailyStudyMinutes);
        dim.put("avgFocusDuration", avgFocusDuration);
        dim.put("avgQuestionTime", (int) avgQuestionTime);
        dim.put("weeklyActiveDays", weeklyActiveDays);

        // Compute real study pattern from activity time distribution
        Map<String, Object> studyPattern = computeStudyPattern(studentId, recentActivities, profile);
        dim.put("studyPattern", studyPattern);
        return dim;
    }

    private Map<String, Object> computeStudyPattern(Long studentId, List<StudyActivity> activities, StudentProfile profile) {
        Map<String, Object> studyPattern = new LinkedHashMap<>();
        int morningCount = 0, afternoonCount = 0, eveningCount = 0;

        for (StudyActivity activity : activities) {
            if (activity.getCreatedAt() == null) continue;
            int hour = activity.getCreatedAt().getHour();
            if (hour >= 6 && hour < 12) morningCount++;
            else if (hour >= 12 && hour < 18) afternoonCount++;
            else eveningCount++;
        }

        int total = morningCount + afternoonCount + eveningCount;
        if (total > 0) {
            studyPattern.put("morning", Math.round((double) morningCount / total * 100.0) / 100.0);
            studyPattern.put("afternoon", Math.round((double) afternoonCount / total * 100.0) / 100.0);
            studyPattern.put("evening", Math.round((double) eveningCount / total * 100.0) / 100.0);
        } else {
            // Fallback based on preferred time slot
            String slot = profile.getPreferredTimeSlot() != null ? profile.getPreferredTimeSlot() : "evening";
            switch (slot) {
                case "morning":
                    studyPattern.put("morning", 0.65);
                    studyPattern.put("afternoon", 0.25);
                    studyPattern.put("evening", 0.10);
                    break;
                case "afternoon":
                    studyPattern.put("morning", 0.15);
                    studyPattern.put("afternoon", 0.60);
                    studyPattern.put("evening", 0.25);
                    break;
                default:
                    studyPattern.put("morning", 0.10);
                    studyPattern.put("afternoon", 0.25);
                    studyPattern.put("evening", 0.65);
            }
        }
        return studyPattern;
    }

    private Map<String, Object> buildCognitiveStyleDimension(StudentProfile profile) {
        Map<String, Object> dim = new LinkedHashMap<>();
        dim.put("label", "认知风格");
        dim.put("description", "偏好的学习内容呈现方式");

        String primary = profile.getLearningStyle() != null
                ? profile.getLearningStyle() : "visual";
        dim.put("primaryStyle", primary);

        // Score based on having a defined learning style and consistent study activity
        Long studentId = profile.getStudentId();
        LocalDate twoWeeksAgo = LocalDate.now().minusWeeks(2);
        int activeDays = studyActivityRepository.countActiveDaysByStudentBetween(studentId, twoWeeksAgo, LocalDate.now());
        // More active days with a defined style = higher confidence in cognitive profile
        double score = profile.getLearningStyle() != null
                ? Math.min(1.0, 0.5 + activeDays * 0.04) : 0.4;
        score = Math.round(score * 100.0) / 100.0;
        dim.put("score", score);

        Map<String, Object> dist = new LinkedHashMap<>();
        dist.put("visual", 0.25);
        dist.put("logical", 0.25);
        dist.put("example", 0.25);
        dist.put("formula", 0.25);
        if (dist.containsKey(primary)) {
            dist.put(primary, 0.45);
            double remaining = 0.55;
            int others = 0;
            for (String k : dist.keySet()) {
                if (!k.equals(primary)) others++;
            }
            double each = Math.round(remaining / others * 100.0) / 100.0;
            for (String k : dist.keySet()) {
                if (!k.equals(primary)) dist.put(k, each);
            }
        }
        dim.put("styleDistribution", dist);

        List<String> contentTypes = new ArrayList<>();
        switch (primary) {
            case "visual":
                contentTypes.addAll(Arrays.asList("图表讲解", "动态演示", "实例分析"));
                break;
            case "logical":
                contentTypes.addAll(Arrays.asList("逻辑推导", "定理证明", "结构化解题"));
                break;
            case "example":
                contentTypes.addAll(Arrays.asList("典型例题", "变式训练", "案例分析"));
                break;
            case "formula":
                contentTypes.addAll(Arrays.asList("公式推导", "速记口诀", "模板套用"));
                break;
            default:
                contentTypes.addAll(Arrays.asList("图表讲解", "动态演示", "实例分析"));
        }
        dim.put("preferredContentTypes", contentTypes);
        return dim;
    }

    private Map<String, Object> buildGoalProfileDimension(StudentProfile profile) {
        Map<String, Object> dim = new LinkedHashMap<>();
        dim.put("label", "总体目标");
        dim.put("description", "专业方向、学习目标与资源偏好；科目掌握情况由科目画像承载");
        dim.put("majorDirection", profile.getMajorDirection());
        dim.put("educationLevel", profile.getEducationLevel());
        dim.put("currentCourse", profile.getCurrentCourse());
        dim.put("learningGoal", profile.getLearningGoal());
        dim.put("foundationLevel", profile.getFoundationLevel());
        dim.put("resourcePreference", parseJsonArray(profile.getResourcePreference()));
        dim.put("academicInterest", profile.getAcademicInterest());
        dim.put("targetSchool", profile.getTargetSchool());
        dim.put("targetScore", profile.getTargetScore());
        dim.put("weakModulePriority", parseJsonArray(profile.getWeakModulePriority()));
        dim.put("overallProfile", buildOverallProfile(profile));

        // Score based on goal completeness: how many goal fields are set
        int filledCount = 0;
        if (profile.getMajorDirection() != null && !profile.getMajorDirection().isBlank()) filledCount++;
        if (profile.getEducationLevel() != null && !profile.getEducationLevel().isBlank()) filledCount++;
        if (profile.getCurrentCourse() != null && !profile.getCurrentCourse().isBlank()) filledCount++;
        if (profile.getLearningGoal() != null && !profile.getLearningGoal().isBlank()) filledCount++;
        if (profile.getFoundationLevel() != null && !profile.getFoundationLevel().isBlank()) filledCount++;
        if (profile.getResourcePreference() != null && !profile.getResourcePreference().isBlank()) filledCount++;
        if (profile.getAcademicInterest() != null && !profile.getAcademicInterest().isBlank()) filledCount++;
        if (profile.getWeakModulePriority() != null && !profile.getWeakModulePriority().isBlank()) filledCount++;
        if (profile.getStudyMode() != null && !profile.getStudyMode().isBlank()) filledCount++;
        if (profile.getDailyStudyMinutes() != null && profile.getDailyStudyMinutes() > 0) filledCount++;
        double score = Math.round(Math.min(1.0, filledCount / 10.0) * 100.0) / 100.0;
        dim.put("score", score);

        dim.put("studyMode", profile.getStudyMode() != null ? profile.getStudyMode() : "systematic");
        return dim;
    }

    /**
     * 维度 6：资源偏好与学习节奏。
     * 刻画学生偏好的资源形态（文档/视频/题库/实操等）、单次学习时长与学习模式，
     * 供多智能体在资源生成与推送时按形态匹配（满足赛题“不少于6个画像维度”要求）。
     */
    private Map<String, Object> buildResourcePreferenceDimension(StudentProfile profile) {
        Map<String, Object> dim = new LinkedHashMap<>();
        dim.put("label", "资源偏好与学习节奏");
        dim.put("description", "偏好的学习资源形态、单次学习时长与学习模式");

        // 偏好的资源形态：取画像中的显式偏好，缺省时按认知风格给出合理默认
        Object parsedPrefs = parseJsonArray(profile.getResourcePreference());
        List<String> prefs = new ArrayList<>();
        if (parsedPrefs instanceof List) {
            for (Object o : (List<?>) parsedPrefs) {
                if (o != null) prefs.add(String.valueOf(o));
            }
        }
        if (prefs.isEmpty()) {
            prefs = defaultResourcePrefByStyle(profile.getLearningStyle());
        }
        dim.put("preferredResourceTypes", prefs);

        // 学习节奏
        Integer dailyMinutes = profile.getDailyStudyMinutes();
        dim.put("dailyStudyMinutes", dailyMinutes != null ? dailyMinutes : 0);
        dim.put("pace", classifyPace(dailyMinutes));
        dim.put("studyMode", profile.getStudyMode() != null ? profile.getStudyMode() : "systematic");

        // 置信度：偏好与节奏字段填充得越完整，分越高
        int filled = 0;
        if (profile.getResourcePreference() != null && !profile.getResourcePreference().isBlank()) filled++;
        if (dailyMinutes != null && dailyMinutes > 0) filled++;
        if (profile.getStudyMode() != null && !profile.getStudyMode().isBlank()) filled++;
        if (profile.getLearningStyle() != null && !profile.getLearningStyle().isBlank()) filled++;
        double score = Math.round(Math.min(1.0, filled / 4.0) * 100.0) / 100.0;
        dim.put("score", score);
        return dim;
    }

    /** 无显式资源偏好时，按认知风格推断默认偏好形态 */
    private List<String> defaultResourcePrefByStyle(String style) {
        String s = style != null ? style : "visual";
        switch (s) {
            case "logical":
                return new ArrayList<>(Arrays.asList("讲解文档", "思维导图", "分层练习"));
            case "example":
                return new ArrayList<>(Arrays.asList("实操案例", "分层练习", "拓展阅读"));
            case "formula":
                return new ArrayList<>(Arrays.asList("讲解文档", "分层练习", "思维导图"));
            case "visual":
            default:
                return new ArrayList<>(Arrays.asList("教学视频", "思维导图", "动画讲解"));
        }
    }

    /** 按单次学习时长粗分学习节奏 */
    private String classifyPace(Integer dailyMinutes) {
        if (dailyMinutes == null || dailyMinutes <= 0) return "unknown";
        if (dailyMinutes < 30) return "short-burst";   // 碎片化、短时高频
        if (dailyMinutes <= 90) return "steady";        // 稳定节奏
        return "deep-dive";                              // 长时深度学习
    }

    private List<Map<String, Object>> buildKnowledgePointCatalog(StudentProfile profile) {
        List<Map<String, Object>> nodes = new ArrayList<>();

        // Parse student mastery state
        Map<String, Double> masteryMap = parseKnowledgeState(profile);

        // Get all modules and their nodes from the knowledge graph
        for (String module : knowledgeGraphService.getAllModules()) {
            String chineseName = mapModuleToChinese(module);
            for (KnowledgeGraphService.KnowledgeNode kn : knowledgeGraphService.getNodesByModule(module)) {
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("id", kn.getId());
                node.put("name", kn.getName());
                node.put("module", chineseName);
                node.put("difficulty", kn.getDifficulty());

                double mastery = masteryMap.getOrDefault(kn.getId(), 0.3);
                node.put("mastery", mastery);
                node.put("masteryLevel", mastery >= 0.8 ? "mastered" : mastery >= 0.5 ? "learning" : "weak");
                node.put("status", mastery >= 0.8 ? "completed" : mastery >= 0.5 ? "learning" : "weak");
                nodes.add(node);
            }
        }
        return nodes;
    }

    private List<Map<String, Object>> buildKnowledgeEdges() {
        List<Map<String, Object>> edges = new ArrayList<>();
        Set<String> allModules = knowledgeGraphService.getAllModules();

        for (String module : allModules) {
            for (KnowledgeGraphService.KnowledgeNode node : knowledgeGraphService.getNodesByModule(module)) {
                if (node.getPrerequisites() != null) {
                    for (String prereqId : node.getPrerequisites()) {
                        KnowledgeGraphService.KnowledgeNode prereq = knowledgeGraphService.getNode(prereqId);
                        if (prereq == null) continue;
                        boolean crossModule = !module.equals(prereq.getModule());
                        Map<String, Object> edge = new LinkedHashMap<>();
                        edge.put("source", prereqId);
                        edge.put("target", node.getId());
                        edge.put("type", "prerequisite");
                        edge.put("weight", 0.9);
                        edge.put("crossModule", crossModule);
                        edges.add(edge);
                    }
                }
            }
        }
        return edges;
    }

    private Map<String, Object> buildPattern(String type, String subType, double frequency, String trend) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("type", type);
        p.put("subType", subType);
        p.put("frequency", frequency);
        p.put("trend", trend);
        return p;
    }

    // ======================== JSON helpers ========================

    private Map<String, Double> parseKnowledgeState(StudentProfile profile) {
        if (profile == null || profile.getKnowledgeState() == null || profile.getKnowledgeState().isBlank()) {
            return Collections.emptyMap();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = OBJECT_MAPPER
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

    private String mapModuleToChinese(String module) {
        switch (module) {
            case "computer-ai-foundation":
            case "ai-foundation":
            case "人工智能基础": return "人工智能基础";
            case "software-java-web":
            case "java-web":
            case "Java Web 开发": return "Java Web 开发";
            case "electronic-digital-circuit":
            case "digital-circuit":
            case "数字电路基础": return "数字电路基础";
            default: return module;
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    private Object parseJsonArray(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return OBJECT_MAPPER.readValue(json, List.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<String> parseStringList(String json) {
        Object raw = parseJsonArray(json);
        if (!(raw instanceof List)) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (Object item : (List<?>) raw) {
            String value = stringOf(item);
            if (value != null && result.stream().noneMatch(existing -> existing.equalsIgnoreCase(value))) {
                result.add(value);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parsePatternList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<?> raw = OBJECT_MAPPER.readValue(json, List.class);
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : raw) {
                if (item instanceof Map) {
                    result.add(new LinkedHashMap<>((Map<String, Object>) item));
                }
            }
            return result;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void clearProfileCache(Long studentId) {
        try {
            redisUtil.delete(PROFILE_CACHE_PREFIX + studentId);
        } catch (Exception ignored) { }
    }
}
