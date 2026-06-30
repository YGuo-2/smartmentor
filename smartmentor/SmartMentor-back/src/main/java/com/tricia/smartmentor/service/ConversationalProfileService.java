package com.tricia.smartmentor.service;

import com.tricia.smartmentor.agent.AgentContext;
import com.tricia.smartmentor.agent.AgentResponse;
import com.tricia.smartmentor.agent.ProfileExtractionAgent;
import com.tricia.smartmentor.entity.StudentProfile;
import com.tricia.smartmentor.repository.StudentProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 对话式画像构建编排：把一段自然语言对话交给 {@link ProfileExtractionAgent} 抽取特征，
 * 再写入学生画像。供「引导访谈结束」（overwrite=true）与「日常对话静默增量」（overwrite=false）复用。
 */
@Slf4j
@Service
public class ConversationalProfileService {

    private final ProfileExtractionAgent profileExtractionAgent;
    private final ProfileService profileService;
    private final StudentProfileRepository studentProfileRepository;

    public ConversationalProfileService(ProfileExtractionAgent profileExtractionAgent,
                                        ProfileService profileService,
                                        StudentProfileRepository studentProfileRepository) {
        this.profileExtractionAgent = profileExtractionAgent;
        this.profileService = profileService;
        this.studentProfileRepository = studentProfileRepository;
    }

    /**
     * 从对话文本抽取画像并写入。
     *
     * @param overwrite true=允许覆盖出厂默认值（引导访谈）；false=仅填补空缺（日常增量）
     * @return {success, message, extractedFeatures, appliedFields}
     */
    public Map<String, Object> extractAndApply(Long studentId, String conversationText, boolean overwrite) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (conversationText == null || conversationText.isBlank()) {
            result.put("success", false);
            result.put("message", "对话内容为空，无法抽取画像");
            return result;
        }

        // 组装 Agent 上下文：带上现有画像供模型参考
        AgentContext context = AgentContext.builder()
                .studentId(studentId)
                .studentProfile(loadExistingProfile(studentId))
                .sessionData(new HashMap<>(Map.of("conversationText", conversationText)))
                .build();

        AgentResponse response = profileExtractionAgent.execute(context);
        if (!response.isSuccess()) {
            result.put("success", false);
            result.put("message", response.getMessage());
            return result;
        }

        Map<String, Object> extracted = response.getData();
        Map<String, Object> applied = profileService.applyExtractedProfile(studentId, extracted, overwrite);

        result.put("success", true);
        result.put("message", applied.isEmpty()
                ? "未发现需要更新的画像特征"
                : "已更新 " + applied.size() + " 项画像特征");
        result.put("extractedFeatures", extracted);
        result.put("appliedFields", applied);
        return result;
    }

    private Map<String, Object> loadExistingProfile(Long studentId) {
        Map<String, Object> map = new HashMap<>();
        StudentProfile p = studentProfileRepository.findByStudentId(studentId).orElse(null);
        if (p == null) {
            return map;
        }
        putIfPresent(map, "majorDirection", p.getMajorDirection());
        putIfPresent(map, "educationLevel", p.getEducationLevel());
        putIfPresent(map, "currentCourse", p.getCurrentCourse());
        putIfPresent(map, "learningGoal", p.getLearningGoal());
        putIfPresent(map, "foundationLevel", p.getFoundationLevel());
        putIfPresent(map, "academicInterest", p.getAcademicInterest());
        putIfPresent(map, "learningStyle", p.getLearningStyle());
        return map;
    }

    private void putIfPresent(Map<String, Object> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }
}
