package com.tricia.smartmentor;

import com.tricia.smartmentor.service.KnowledgeGraphService;
import com.tricia.smartmentor.service.MasteryUpdateService;
import com.tricia.smartmentor.service.OfflineDemoService;
import com.tricia.smartmentor.service.LearningService;
import com.tricia.smartmentor.service.PromptTemplateService;
import com.tricia.smartmentor.service.QuestionBankService;
import com.tricia.smartmentor.service.AuthService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SmartMentorApplicationTests {

    @Autowired
    private KnowledgeGraphService knowledgeGraphService;

    @Autowired
    private MasteryUpdateService masteryUpdateService;

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Autowired
    private OfflineDemoService offlineDemoService;

    @Autowired
    private QuestionBankService questionBankService;

    @Autowired
    private LearningService learningService;

    @Autowired
    private AuthService authService;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Autowired
    private com.tricia.smartmentor.repository.StudentProfileRepository studentProfileRepository;

    @Autowired
    private com.tricia.smartmentor.repository.MasteryHistoryRepository masteryHistoryRepository;

    @Autowired
    private com.tricia.smartmentor.repository.StudentRepository studentRepository;

    @Autowired
    private com.tricia.smartmentor.repository.QuestionBankRepository questionBankRepository;

    @Autowired
    private com.tricia.smartmentor.repository.LearningPathRepository learningPathRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void knowledgeGraphLoadsChineseModulesAndMetadata() {
        Assertions.assertFalse(knowledgeGraphService.getNodesByModule("人工智能基础").isEmpty());
        Assertions.assertFalse(knowledgeGraphService.getNodesByModule("Java Web 开发").isEmpty());

        KnowledgeGraphService.KnowledgeNode node = knowledgeGraphService.getNode("ai_intro");
        Assertions.assertNotNull(node);
        Assertions.assertEquals("人工智能概述", node.getName());
        Assertions.assertNotNull(node.getCommonErrors());
        Assertions.assertFalse(node.getCommonErrors().isEmpty());
        Assertions.assertTrue(node.getExamWeight() > 0);
        Assertions.assertNotNull(node.getEstimatedMinutes());
    }

    @Test
    void masteryUpdateWritesProfileAndHistory() {
        Long studentId = 90001L;

        double masteryAfter = masteryUpdateService.updateFromAnswer(
                studentId, "ai_intro", "人工智能基础", true, "test");

        Assertions.assertTrue(masteryAfter > 0.3);

        com.tricia.smartmentor.entity.StudentProfile profile = studentProfileRepository
                .findByStudentId(studentId)
                .orElseThrow();

        Assertions.assertNotNull(profile.getKnowledgeState());
        Assertions.assertTrue(profile.getKnowledgeState().contains("ai_intro"));
        Assertions.assertNotNull(profile.getOverallMastery());

        java.util.List<com.tricia.smartmentor.entity.MasteryHistory> history =
                masteryHistoryRepository.findLatestByStudentAndKp(studentId, "ai_intro");

        Assertions.assertFalse(history.isEmpty());
        Assertions.assertEquals("test", history.get(0).getSource());
    }

    @Test
    void promptTemplatesLoadWithVersions() {
        PromptTemplateService.PromptTemplate template = promptTemplateService.load(
                "diagnostic-system", "fallback", "fallback content");

        Assertions.assertEquals("diagnostic-system-v1", template.getVersion());
        Assertions.assertTrue(template.getContent().contains("诊断Agent"));
        Assertions.assertFalse(template.getContent().contains("---"));
    }

    @Test
    void offlineDemoReturnsDiagnosticQuestions() throws Exception {
        String response = offlineDemoService.buildAgentResponse("诊断Agent", "请为以下高校课程模块生成8道自适应诊断题目");

        java.util.Map<?, ?> parsed = new com.fasterxml.jackson.databind.ObjectMapper().readValue(response, java.util.Map.class);
        Object questions = parsed.get("questions");
        Assertions.assertTrue(questions instanceof java.util.List);
        Assertions.assertEquals(8, ((java.util.List<?>) questions).size());
    }

    @Test
    void questionBankDedupesAndReturnsReusableQuestions() {
        String sourceRef = "reuse_test_" + java.util.UUID.randomUUID();
        String kpId = "reuse_kp_" + java.util.UUID.randomUUID();
        java.util.Map<String, Object> question = new java.util.LinkedHashMap<>();
        question.put("knowledgePointId", kpId);
        question.put("knowledgePointName", "题库复用知识点");
        question.put("type", "choice");
        question.put("difficulty", 0.8);
        question.put("content", "题库复用诊断题");
        question.put("options", java.util.Map.of("A", "1", "B", "2", "C", "3", "D", "4"));
        question.put("answer", "B");
        question.put("explanation", "题库复用解析");

        questionBankService.saveGeneratedQuestions("test_reuse", "测试模块", sourceRef, java.util.List.of(question));
        questionBankService.saveGeneratedQuestions("test_reuse", "测试模块", sourceRef, java.util.List.of(question));

        Assertions.assertEquals(1L, questionBankRepository.countBySourceRef(sourceRef));

        java.util.List<java.util.Map<String, Object>> reusable =
                questionBankService.findReusableDiagnosticQuestions(java.util.List.of(kpId), 5);
        Assertions.assertFalse(reusable.isEmpty());
        Assertions.assertEquals("题库复用诊断题", reusable.get(0).get("question"));
        Assertions.assertEquals("B", reusable.get(0).get("correctAnswer"));
        Assertions.assertEquals(new java.math.BigDecimal("4.0"), reusable.get(0).get("difficulty"));
    }

    @Test
    void checkpointSubmitUsesServerSnapshotInsteadOfClientCorrectAnswer() throws Exception {
        Long studentId = 93001L;
        com.tricia.smartmentor.entity.LearningPath path = new com.tricia.smartmentor.entity.LearningPath();
        path.setStudentId(studentId);
        path.setTargetKnowledgePointId("ai_intro");
        path.setTargetKnowledgePointName("人工智能概述");
        path.setRootCausePointId("ai_intro");
        path.setRootCausePointName("人工智能概述");
        path.setMode("systematic");
        path.setStatus("active");
        path.setTotalNodes(1);
        path.setCompletedNodes(0);
        path.setNodes(objectMapper.writeValueAsString(java.util.List.of(java.util.Map.of(
                "nodeId", "node_1",
                "knowledgePointId", "ai_intro",
                "knowledgePointName", "人工智能概述",
                "currentMastery", 0.2,
                "status", "pending"
        ))));
        path.setLessonSnapshots(objectMapper.writeValueAsString(java.util.Map.of(
                "node_1", java.util.Map.of(
                        "lesson", java.util.Map.of(
                                "exercises", java.util.List.of(java.util.Map.of(
                                        "exerciseId", "cp_1",
                                        "question", "人工智能概述检查题",
                                        "correctAnswer", "A",
                                        "difficulty", 1,
                                        "explanation", "服务端快照答案为 A"
                                ))
                        )
                )
        )));
        path = learningPathRepository.save(path);

        java.util.Map<String, Object> submitted = new java.util.LinkedHashMap<>();
        submitted.put("exerciseId", "cp_1");
        submitted.put("answer", "B");
        submitted.put("correctAnswer", "B");

        java.util.Map<String, Object> result = learningService.submitCheckpoint(
                studentId, path.getId(), "node_1", java.util.List.of(submitted), 12);

        Assertions.assertTrue(Boolean.TRUE.equals(result.get("serverSnapshotUsed")));
        Assertions.assertEquals(0, ((Number) result.get("correctCount")).intValue());
        java.util.List<?> results = (java.util.List<?>) result.get("results");
        Assertions.assertFalse((Boolean) ((java.util.Map<?, ?>) results.get(0)).get("isCorrect"));
        java.util.Map<?, ?> nextAction = (java.util.Map<?, ?>) result.get("nextAction");
        Assertions.assertEquals("remediation", nextAction.get("type"));

        com.tricia.smartmentor.entity.LearningPath updated = learningPathRepository.findById(path.getId()).orElseThrow();
        Assertions.assertTrue(updated.getNodes().contains("remedial_node_1"));
    }

    @Test
    void checkpointPassReturnsUnlockedCurrentNodeAsNextAction() throws Exception {
        Long studentId = 93002L;
        com.tricia.smartmentor.entity.LearningPath path = new com.tricia.smartmentor.entity.LearningPath();
        path.setStudentId(studentId);
        path.setTargetKnowledgePointId("ai_knowledge_representation");
        path.setTargetKnowledgePointName("知识表示");
        path.setRootCausePointId("ai_intro");
        path.setRootCausePointName("人工智能概述");
        path.setMode("systematic");
        path.setStatus("active");
        path.setTotalNodes(2);
        path.setCompletedNodes(0);
        path.setNodes(objectMapper.writeValueAsString(java.util.List.of(
                java.util.Map.of(
                        "nodeId", "node_1",
                        "knowledgePointId", "ai_intro",
                        "knowledgePointName", "人工智能概述",
                        "currentMastery", 0.2,
                        "status", "in_progress"
                ),
                java.util.Map.of(
                        "nodeId", "node_2",
                        "knowledgePointId", "ai_knowledge_representation",
                        "knowledgePointName", "知识表示",
                        "currentMastery", 0.0,
                        "status", "locked"
                )
        )));
        path.setLessonSnapshots(objectMapper.writeValueAsString(java.util.Map.of(
                "node_1", java.util.Map.of(
                        "lesson", java.util.Map.of(
                                "exercises", java.util.List.of(java.util.Map.of(
                                        "exerciseId", "cp_1",
                                        "question", "人工智能概述检查题",
                                        "correctAnswer", "A",
                                        "difficulty", 1,
                                        "explanation", "服务端快照答案为 A"
                                ))
                        )
                )
        )));
        path = learningPathRepository.save(path);

        java.util.Map<String, Object> submitted = new java.util.LinkedHashMap<>();
        submitted.put("exerciseId", "cp_1");
        submitted.put("answer", "A");

        java.util.Map<String, Object> result = learningService.submitCheckpoint(
                studentId, path.getId(), "node_1", java.util.List.of(submitted), 12);

        Assertions.assertTrue((Boolean) result.get("passed"));
        Assertions.assertEquals("node_2", result.get("currentNodeId"));
        Assertions.assertEquals("node_2", result.get("nextNodeId"));
        java.util.Map<?, ?> nextAction = (java.util.Map<?, ?>) result.get("nextAction");
        Assertions.assertEquals("next_node", nextAction.get("type"));
        Assertions.assertEquals("node_2", nextAction.get("nextNodeId"));

        com.tricia.smartmentor.entity.LearningPath updated = learningPathRepository.findById(path.getId()).orElseThrow();
        Assertions.assertEquals("node_2", updated.getCurrentNodeId());
        java.util.List<?> nodes = readJsonList(updated.getNodes());
        Assertions.assertEquals("completed", ((java.util.Map<?, ?>) nodes.get(0)).get("status"));
        Assertions.assertEquals("unlocked", ((java.util.Map<?, ?>) nodes.get(1)).get("status"));
    }

    @Test
    void checkpointPassExtendsSingleNodePathWhenGraphHasDependents() throws Exception {
        Long studentId = 93003L;
        com.tricia.smartmentor.entity.LearningPath path = new com.tricia.smartmentor.entity.LearningPath();
        path.setStudentId(studentId);
        path.setTargetKnowledgePointId("ai_intro");
        path.setTargetKnowledgePointName("人工智能概述");
        path.setRootCausePointId("ai_intro");
        path.setRootCausePointName("人工智能概述");
        path.setMode("systematic");
        path.setStatus("active");
        path.setTotalNodes(1);
        path.setCompletedNodes(0);
        path.setNodes(objectMapper.writeValueAsString(java.util.List.of(java.util.Map.of(
                "nodeId", "node_1",
                "knowledgePointId", "ai_intro",
                "knowledgePointName", "人工智能概述",
                "module", "人工智能基础",
                "currentMastery", 0.2,
                "status", "in_progress"
        ))));
        path.setLessonSnapshots(objectMapper.writeValueAsString(java.util.Map.of(
                "node_1", java.util.Map.of(
                        "lesson", java.util.Map.of(
                                "exercises", java.util.List.of(java.util.Map.of(
                                        "exerciseId", "cp_1",
                                        "question", "人工智能概述检查题",
                                        "correctAnswer", "A",
                                        "difficulty", 1,
                                        "explanation", "服务端快照答案为 A"
                                ))
                        )
                )
        )));
        path = learningPathRepository.save(path);

        java.util.Map<String, Object> submitted = new java.util.LinkedHashMap<>();
        submitted.put("exerciseId", "cp_1");
        submitted.put("answer", "A");

        java.util.Map<String, Object> result = learningService.submitCheckpoint(
                studentId, path.getId(), "node_1", java.util.List.of(submitted), 12);

        Assertions.assertTrue((Boolean) result.get("passed"));
        Assertions.assertEquals("next_node", ((java.util.Map<?, ?>) result.get("nextAction")).get("type"));
        Assertions.assertNotNull(result.get("nextNodeId"));
        Assertions.assertNotEquals("node_1", result.get("nextNodeId"));

        com.tricia.smartmentor.entity.LearningPath updated = learningPathRepository.findById(path.getId()).orElseThrow();
        Assertions.assertEquals("active", updated.getStatus());
        Assertions.assertEquals(result.get("nextNodeId"), updated.getCurrentNodeId());
        java.util.List<?> nodes = readJsonList(updated.getNodes());
        Assertions.assertTrue(nodes.size() > 1);
        Assertions.assertEquals("completed", ((java.util.Map<?, ?>) nodes.get(0)).get("status"));
        Assertions.assertEquals("unlocked", ((java.util.Map<?, ?>) nodes.get(1)).get("status"));
    }

    @Test
    void loginDefaultsToStudentWhenRoleIsOmitted() {
        String username = "roleless_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        com.tricia.smartmentor.entity.Student student = createStudent(username, "无角色登录用户");
        student.setEmail(username + "@example.com");
        student.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("secret123"));
        studentRepository.save(student);

        com.tricia.smartmentor.dto.LoginRequest request = new com.tricia.smartmentor.dto.LoginRequest();
        request.setUsername(username);
        request.setPassword("secret123");

        com.tricia.smartmentor.dto.AuthResponse response = authService.login(request);

        Assertions.assertEquals("student", response.getRole());
        Assertions.assertEquals(username, response.getUsername());
        Assertions.assertEquals(username + "@example.com", response.getEmail());
        Assertions.assertNotNull(response.getToken());
    }

    private java.util.List<?> readJsonList(String json) throws Exception {
        try {
            return objectMapper.readValue(json, java.util.List.class);
        } catch (com.fasterxml.jackson.databind.exc.MismatchedInputException e) {
            return objectMapper.readValue(objectMapper.readValue(json, String.class), java.util.List.class);
        }
    }

    private com.tricia.smartmentor.entity.Student createStudent(String username, String nickname) {
        com.tricia.smartmentor.entity.Student student = new com.tricia.smartmentor.entity.Student();
        student.setUsername(username);
        student.setPassword("test-password");
        student.setNickname(nickname);
        student.setGrade("本科");
        return studentRepository.save(student);
    }

    private void createProfile(Long studentId, String mastery) {
        com.tricia.smartmentor.entity.StudentProfile profile = new com.tricia.smartmentor.entity.StudentProfile();
        profile.setStudentId(studentId);
        profile.setOverallMastery(new java.math.BigDecimal(mastery));
        studentProfileRepository.save(profile);
    }

}
