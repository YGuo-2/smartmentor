package com.tricia.smartmentor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tricia.smartmentor.entity.QuestionBank;
import com.tricia.smartmentor.repository.QuestionBankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionBankService {

    private final QuestionBankRepository questionBankRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public int saveGeneratedQuestions(String source,
                                      String module,
                                      String sourceRef,
                                      List<Map<String, Object>> questions) {
        if (questions == null || questions.isEmpty()) {
            return 0;
        }

        int saved = 0;
        for (Map<String, Object> question : questions) {
            String content = firstString(question, "question", "content", "title");
            if (content.isBlank()) {
                continue;
            }
            String correctAnswer = firstString(question, "correctAnswer", "answer");
            String knowledgePointId = firstString(question, "knowledgePointId", "kpId");
            String questionHash = sha256(source + "|" + knowledgePointId + "|" + content + "|" + correctAnswer);

            QuestionBank bankQuestion = questionBankRepository.findByQuestionHash(questionHash)
                    .orElseGet(QuestionBank::new);
            bankQuestion.setQuestionHash(questionHash);
            bankQuestion.setSource(source);
            bankQuestion.setSourceRef(sourceRef);
            bankQuestion.setModule(module);
            bankQuestion.setKnowledgePointId(knowledgePointId);
            bankQuestion.setKnowledgePointName(firstString(question, "knowledgePointName", "kpName"));
            bankQuestion.setQuestionType(firstString(question, "questionType", "type"));
            bankQuestion.setDifficulty(readDifficulty(question.get("difficulty")));
            bankQuestion.setContent(content);
            bankQuestion.setOptions(toJson(question.get("options")));
            bankQuestion.setCorrectAnswer(correctAnswer);
            bankQuestion.setExplanation(firstString(question, "explanation", "analysis"));
            bankQuestion.setErrorType(firstString(question, "errorType", "commonErrorFocus"));
            bankQuestion.setQualityScore(estimateQualityScore(question));
            questionBankRepository.save(bankQuestion);
            saved++;
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> findReusableDiagnosticQuestions(Collection<String> knowledgePointIds, int limit) {
        return findReusableQuestions(knowledgePointIds, limit, true);
    }

    private List<Map<String, Object>> findReusableQuestions(Collection<String> knowledgePointIds,
                                                            int limit,
                                                            boolean diagnosticScale) {
        if (knowledgePointIds == null || knowledgePointIds.isEmpty() || limit <= 0) {
            return Collections.emptyList();
        }
        List<String> sanitized = knowledgePointIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        if (sanitized.isEmpty()) {
            return Collections.emptyList();
        }

        List<QuestionBank> bankQuestions = questionBankRepository.findReusableByKnowledgePointIds(
                sanitized, PageRequest.of(0, limit));
        List<Map<String, Object>> questions = new ArrayList<>();
        for (QuestionBank bankQuestion : bankQuestions) {
            questions.add(toQuestionMap(bankQuestion, diagnosticScale));
        }
        return questions;
    }

    private Map<String, Object> toQuestionMap(QuestionBank bankQuestion, boolean diagnosticScale) {
        Map<String, Object> question = new LinkedHashMap<>();
        question.put("id", "bank_" + bankQuestion.getId());
        question.put("questionBankId", bankQuestion.getId());
        question.put("source", "question_bank");
        question.put("knowledgePointId", nullToEmpty(bankQuestion.getKnowledgePointId()));
        question.put("knowledgePointName", nullToEmpty(bankQuestion.getKnowledgePointName()));
        question.put("type", nullToDefault(bankQuestion.getQuestionType(), "choice"));
        question.put("questionType", nullToDefault(bankQuestion.getQuestionType(), "choice"));
        question.put("question", nullToEmpty(bankQuestion.getContent()));
        question.put("content", nullToEmpty(bankQuestion.getContent()));
        question.put("options", parseOptions(bankQuestion.getOptions()));
        question.put("correctAnswer", nullToEmpty(bankQuestion.getCorrectAnswer()));
        question.put("answer", nullToEmpty(bankQuestion.getCorrectAnswer()));
        question.put("explanation", nullToEmpty(bankQuestion.getExplanation()));
        question.put("errorType", nullToEmpty(bankQuestion.getErrorType()));
        question.put("commonErrorFocus", nullToEmpty(bankQuestion.getErrorType()));
        question.put("qualityScore", bankQuestion.getQualityScore());

        BigDecimal difficulty = bankQuestion.getDifficulty();
        if (difficulty != null) {
            if (diagnosticScale) {
                question.put("difficulty", difficulty.multiply(BigDecimal.valueOf(5))
                        .setScale(1, RoundingMode.HALF_UP));
            } else {
                question.put("difficulty", difficulty);
            }
        }
        return question;
    }

    private String firstString(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object value = data.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value).trim();
            }
        }
        return "";
    }

    private BigDecimal readDifficulty(Object value) {
        if (value instanceof Number) {
            double raw = ((Number) value).doubleValue();
            if (raw > 1.0) {
                raw = raw / 5.0;
            }
            return BigDecimal.valueOf(Math.max(0.0, Math.min(1.0, raw))).setScale(2, java.math.RoundingMode.HALF_UP);
        }
        return null;
    }

    private BigDecimal estimateQualityScore(Map<String, Object> question) {
        int score = 0;
        if (!firstString(question, "question", "content", "title").isBlank()) score += 25;
        if (!firstString(question, "correctAnswer", "answer").isBlank()) score += 25;
        if (question.get("options") != null) score += 20;
        if (!firstString(question, "explanation", "analysis").isBlank()) score += 20;
        if (!firstString(question, "knowledgePointId", "kpId").isBlank()) score += 10;
        return BigDecimal.valueOf(score / 100.0).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private Object parseOptions(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    private String nullToDefault(String value, String defaultValue) {
        return value != null && !value.isBlank() ? value : defaultValue;
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            log.warn("Question hash generation failed: {}", e.getMessage());
            return String.valueOf(text.hashCode());
        }
    }
}
