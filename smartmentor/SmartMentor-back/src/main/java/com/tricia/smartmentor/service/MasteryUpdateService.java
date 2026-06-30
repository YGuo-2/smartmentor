package com.tricia.smartmentor.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tricia.smartmentor.entity.MasteryHistory;
import com.tricia.smartmentor.entity.StudentProfile;
import com.tricia.smartmentor.repository.MasteryHistoryRepository;
import com.tricia.smartmentor.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MasteryUpdateService {

    private static final double DEFAULT_MASTERY = 0.3;
    private static final double P_SLIP = 0.15;
    private static final double P_GUESS = 0.1;
    private static final double P_TRANSIT = 0.2;

    private final StudentProfileRepository studentProfileRepository;
    private final MasteryHistoryRepository masteryHistoryRepository;
    private final KnowledgeGraphService knowledgeGraphService;
    private final ObjectMapper objectMapper;

    @Transactional
    public double updateFromAnswer(Long studentId,
                                   String knowledgePointId,
                                   String module,
                                   boolean correct,
                                   String source) {
        if (knowledgePointId == null || knowledgePointId.isBlank()) {
            return DEFAULT_MASTERY;
        }

        StudentProfile profile = studentProfileRepository.findByStudentId(studentId)
                .orElseGet(() -> createProfile(studentId));

        Map<String, Double> knowledgeState = parseKnowledgeState(profile.getKnowledgeState());
        double previousMastery = knowledgeState.getOrDefault(knowledgePointId, DEFAULT_MASTERY);
        double nextMastery = updateBkt(previousMastery, correct);
        knowledgeState.put(knowledgePointId, round4(nextMastery));

        double overallMastery = calculateOverallMastery(knowledgeState);
        profile.setKnowledgeState(toJson(knowledgeState));
        profile.setOverallMastery(toBigDecimal(overallMastery, 2));
        studentProfileRepository.save(profile);

        MasteryHistory history = new MasteryHistory();
        history.setStudentId(studentId);
        history.setKnowledgePointId(knowledgePointId);
        history.setModule(resolveModule(module, knowledgePointId));
        history.setMastery(toBigDecimal(nextMastery, 4));
        history.setOverallMastery(toBigDecimal(overallMastery, 4));
        history.setSource(source);
        masteryHistoryRepository.save(history);

        return nextMastery;
    }

    @Transactional
    public void recordKnowledgePointMastery(Long studentId,
                                            String knowledgePointId,
                                            String module,
                                            double mastery,
                                            String source) {
        if (knowledgePointId == null || knowledgePointId.isBlank()) {
            return;
        }

        StudentProfile profile = studentProfileRepository.findByStudentId(studentId)
                .orElseGet(() -> createProfile(studentId));

        Map<String, Double> knowledgeState = parseKnowledgeState(profile.getKnowledgeState());
        double normalizedMastery = clamp(mastery);
        knowledgeState.put(knowledgePointId, round4(normalizedMastery));

        double overallMastery = calculateOverallMastery(knowledgeState);
        profile.setKnowledgeState(toJson(knowledgeState));
        profile.setOverallMastery(toBigDecimal(overallMastery, 2));
        studentProfileRepository.save(profile);

        MasteryHistory history = new MasteryHistory();
        history.setStudentId(studentId);
        history.setKnowledgePointId(knowledgePointId);
        history.setModule(resolveModule(module, knowledgePointId));
        history.setMastery(toBigDecimal(normalizedMastery, 4));
        history.setOverallMastery(toBigDecimal(overallMastery, 4));
        history.setSource(source);
        masteryHistoryRepository.save(history);
    }

    private StudentProfile createProfile(Long studentId) {
        StudentProfile profile = new StudentProfile();
        profile.setStudentId(studentId);
        return studentProfileRepository.save(profile);
    }

    private double updateBkt(double currentMastery, boolean correct) {
        double pL = clamp(currentMastery);
        double posterior;
        if (correct) {
            double numerator = pL * (1 - P_SLIP);
            double denominator = numerator + (1 - pL) * P_GUESS;
            posterior = denominator > 0 ? numerator / denominator : pL;
        } else {
            double numerator = pL * P_SLIP;
            double denominator = numerator + (1 - pL) * (1 - P_GUESS);
            posterior = denominator > 0 ? numerator / denominator : pL;
        }
        return clamp(posterior + (1 - posterior) * P_TRANSIT);
    }

    private double calculateOverallMastery(Map<String, Double> knowledgeState) {
        if (knowledgeState.isEmpty()) {
            return 0.0;
        }
        return knowledgeState.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    private String resolveModule(String module, String knowledgePointId) {
        if (module != null && !module.isBlank()) {
            return module;
        }
        KnowledgeGraphService.KnowledgeNode node = knowledgeGraphService.getNode(knowledgePointId);
        return node != null ? node.getModule() : null;
    }

    private Map<String, Double> parseKnowledgeState(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            Map<String, Double> result = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    result.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
                }
            }
            return result;
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private BigDecimal toBigDecimal(double value, int scale) {
        return BigDecimal.valueOf(clamp(value)).setScale(scale, RoundingMode.HALF_UP);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private double round4(double value) {
        return BigDecimal.valueOf(clamp(value)).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}
