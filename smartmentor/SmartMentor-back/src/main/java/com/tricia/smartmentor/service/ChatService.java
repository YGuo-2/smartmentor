package com.tricia.smartmentor.service;

import com.tricia.smartmentor.entity.*;
import com.tricia.smartmentor.repository.*;
import com.tricia.smartmentor.util.RedisUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class ChatService {
    private static final String ACTION_CARD_KEY_PREFIX = "chat:actionCard:";

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final StudentRepository studentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final LearningPathRepository learningPathRepository;
    private final DiagnosticSessionRepository diagnosticSessionRepository;
    private final AnswerRecordRepository answerRecordRepository;
    private final LlmService llmService;
    private final KnowledgeGraphService knowledgeGraphService;
    private final BilibiliVideoService bilibiliVideoService;
    private final ContentSafetyService contentSafetyService;
    private final ConversationalProfileService conversationalProfileService;
    private final MasteryUpdateService masteryUpdateService;
    private final ProfileService profileService;
    private final MemoryService memoryService;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService heartbeatScheduler =
            Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r, "sse-heartbeat");
                t.setDaemon(true);
                return t;
            });
    // 日常对话画像静默增量更新：异步执行，绝不阻塞 SSE
    private final ExecutorService profileUpdateExecutor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "chat-profile-update");
                t.setDaemon(true);
                return t;
            });

    public ChatService(ChatSessionRepository chatSessionRepository,
                       ChatMessageRepository chatMessageRepository,
                       StudentRepository studentRepository,
                       StudentProfileRepository studentProfileRepository,
                       LearningPathRepository learningPathRepository,
                       DiagnosticSessionRepository diagnosticSessionRepository,
                       AnswerRecordRepository answerRecordRepository,
                       LlmService llmService,
                       KnowledgeGraphService knowledgeGraphService,
                       BilibiliVideoService bilibiliVideoService,
                       ContentSafetyService contentSafetyService,
                       ConversationalProfileService conversationalProfileService,
                       MasteryUpdateService masteryUpdateService,
                       ProfileService profileService,
                       MemoryService memoryService,
                       RedisUtil redisUtil,
                       ObjectMapper objectMapper) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.studentRepository = studentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.learningPathRepository = learningPathRepository;
        this.diagnosticSessionRepository = diagnosticSessionRepository;
        this.answerRecordRepository = answerRecordRepository;
        this.llmService = llmService;
        this.knowledgeGraphService = knowledgeGraphService;
        this.bilibiliVideoService = bilibiliVideoService;
        this.contentSafetyService = contentSafetyService;
        this.conversationalProfileService = conversationalProfileService;
        this.masteryUpdateService = masteryUpdateService;
        this.profileService = profileService;
        this.memoryService = memoryService;
        this.redisUtil = redisUtil;
        this.objectMapper = objectMapper;
    }

    /**
     * Stream AI tutoring response via SSE, powered by DeepSeek
     */
    public SseEmitter streamResponse(Long studentId, String message, String sessionId,
                                     Long pathId, String nodeId, String mode) {
        boolean interviewMode = "profile_interview".equals(mode);
        SseEmitter emitter = new SseEmitter(180_000L);
        AtomicBoolean alive = new AtomicBoolean(true);

        // 心跳：每 15s 发一个注释行保活，防止代理/Nginx 超时断开
        ScheduledFuture<?> heartbeat = heartbeatScheduler.scheduleAtFixedRate(() -> {
            if (!alive.get()) return;
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException e) {
                alive.set(false);
            }
        }, 15, 15, TimeUnit.SECONDS);

        emitter.onCompletion(() -> { alive.set(false); heartbeat.cancel(false); });
        emitter.onTimeout(() -> { alive.set(false); heartbeat.cancel(false); emitter.complete(); });
        emitter.onError(t -> { alive.set(false); heartbeat.cancel(false); });

        // 内容安全：输入拦截。命中违规内容直接拒绝，不进入大模型（满足赛题内容安全过滤要求）
        if (!contentSafetyService.isInputAllowed(message)) {
            try {
                emitter.send(SseEmitter.event().name("message")
                        .data(Map.of("content", ContentSafetyService.BLOCKED_HINT)));
                emitter.send(SseEmitter.event().name("done")
                        .data(Map.of("sessionId", sessionId == null ? "" : sessionId, "blocked", true)));
            } catch (IOException ignored) {
            } finally {
                alive.set(false);
                heartbeat.cancel(false);
                emitter.complete();
            }
            return emitter;
        }

        // === Session 处理 ===
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = "s_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        final String sid = sessionId;

        ChatSession session = chatSessionRepository.findBySessionId(sid).orElse(null);
        if (session == null) {
            session = new ChatSession();
            session.setSessionId(sid);
            session.setStudentId(studentId);
            session.setTitle(message.length() > 50 ? message.substring(0, 50) + "..." : message);
            session.setPathId(pathId);
            session.setNodeId(nodeId);
            session.setKnowledgePointName(resolveKnowledgePointName(studentId, pathId, nodeId));
            session.setMessageCount(0);
            session = chatSessionRepository.save(session);
        } else {
            assertSessionOwner(session, studentId);
        }

        // 保存学生消息
        ChatMessage studentMsg = new ChatMessage();
        studentMsg.setMessageId("m_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        studentMsg.setSessionId(sid);
        studentMsg.setStudentId(studentId);
        studentMsg.setRole("student");
        studentMsg.setContent(message);
        chatMessageRepository.save(studentMsg);

        session.setMessageCount(session.getMessageCount() + 1);
        session.setLastMessage(message);
        session.setLastActiveAt(LocalDateTime.now());
        chatSessionRepository.save(session);

        final ChatSession fSession = session;

        // === 构建上下文 ===
        List<Map<String, String>> apiMessages = buildContextMessages(studentId, sid, message, pathId, nodeId, interviewMode);

        // === 学习资源检索：当学生在对话中索要资料/视频时，检索并推送资源卡片 ===
        final List<Map<String, Object>> learningResources = new ArrayList<>();
        if (wantsLearningResource(message)) {
            String query = resolveResourceQuery(studentId, sid, message, fSession.getKnowledgePointName());
            if (query != null && !query.isBlank()) {
                try {
                    learningResources.addAll(bilibiliVideoService.searchLearningVideos(query, 5));
                } catch (Exception e) {
                    log.warn("对话资源检索失败: {}", e.getMessage());
                }
            }
            if (!learningResources.isEmpty()) {
                // 让 AI 在回答中自然引出这些资源
                StringBuilder hint = new StringBuilder();
                hint.append("【系统已为学生检索到以下学习视频资源，请在回答中用一两句话简要介绍它们的用途，")
                    .append("并提示学生可在下方资源卡片中查看，不要罗列链接】\n");
                for (int i = 0; i < learningResources.size(); i++) {
                    Map<String, Object> r = learningResources.get(i);
                    hint.append(i + 1).append(". ").append(stringValue(r.get("title")))
                        .append("（UP主：").append(stringValue(r.get("author"))).append("）\n");
                }
                apiMessages.add(Map.of("role", "system", "content", hint.toString()));
            }
        }

        String aiMsgId = "m_ai_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        StringBuilder fullResponse = new StringBuilder();

        // 发送 metadata
        try {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("sessionId", sid);
            metadata.put("knowledgeContext", fSession.getKnowledgePointName() != null
                    ? fSession.getKnowledgePointName() : "课程知识点");
            Map<String, Object> actionCard = consumeActionCard(studentId);
            if (actionCard != null && !actionCard.isEmpty()) {
                metadata.put("actionCard", actionCard);
            }
            emitter.send(SseEmitter.event().name("metadata").data(metadata));
        } catch (IOException e) {
            log.warn("metadata 发送失败");
        }

        // 推送资源卡片事件
        final String resourcesJson = serializeResources(learningResources);
        if (!learningResources.isEmpty()) {
            try {
                emitter.send(SseEmitter.event().name("resources").data(
                        Map.of("sessionId", sid, "resources", learningResources)));
            } catch (IOException e) {
                log.warn("resources 事件发送失败");
            }
        }

        // === 流式调用 DeepSeek ===
        llmService.streamChat(
                apiMessages,
                0.7,
                // onToken — 用 JSON 包裹，避免 token 中的换行符破坏 SSE 帧格式
                token -> {
                    if (!alive.get()) return;
                    fullResponse.append(token);
                    try {
                        emitter.send(SseEmitter.event().name("message")
                                .data(Map.of("content", token)));
                    } catch (IOException e) {
                        alive.set(false);
                    }
                },
                // onComplete
                () -> {
                    if (!alive.get()) {
                        saveAiMessage(aiMsgId, sid, fSession, fullResponse, resourcesJson);
                        maybeUpdateProfileFromChat(studentId, sid, interviewMode, fSession);
                        return;
                    }
                    try {
                        saveAiMessage(aiMsgId, sid, fSession, fullResponse, resourcesJson);
                        emitter.send(SseEmitter.event().name("done").data(
                                Map.of("sessionId", sid, "messageId", aiMsgId)));
                        emitter.complete();
                    } catch (IOException e) {
                        emitter.complete();
                    }
                    maybeUpdateProfileFromChat(studentId, sid, interviewMode, fSession);
                },
                // onError
                error -> {
                    log.error("DeepSeek 调用失败: {}", error.getMessage());
                    // 如果已有部分输出，仍然保存
                    if (fullResponse.length() > 0) {
                        saveAiMessage(aiMsgId, sid, fSession, fullResponse, resourcesJson);
                    }
                    if (alive.get()) {
                        try {
                            emitter.send(SseEmitter.event().name("error")
                                    .data(Map.of("error", "AI 服务暂时不可用，请稍后再试")));
                        } catch (IOException ignored) {}
                        emitter.complete();
                    }
                }
        );

        return emitter;
    }

    // ======================== 上下文构建 ========================

    /**
     * 构建完整的 API messages 数组：
     * 1. system prompt（含学生画像 + 知识点上下文）
     * 2. 历史对话（滑动窗口，最多 10 轮 = 20 条）
     * 3. 当前用户消息
     */
    private List<Map<String, String>> buildContextMessages(Long studentId, String sessionId,
                                                           String currentMessage, Long pathId, String nodeId,
                                                           boolean interviewMode) {
        List<Map<String, String>> messages = new ArrayList<>();

        // 1. System prompt 含动态上下文
        String systemPrompt = interviewMode
                ? buildInterviewSystemPrompt(studentId)
                : buildSystemPrompt(studentId, pathId, nodeId);
        messages.add(Map.of("role", "system", "content", systemPrompt));

        // 1.5 长期记忆召回：按当前消息语义/关键词召回历史会话沉淀的记忆，注入为一条 system 消息。
        // 访谈模式跳过（画像采集不应被旧记忆干扰）；recall 内部有超时+降级，不阻塞首包。
        if (!interviewMode) {
            String memoryContext = memoryService.recall(studentId, currentMessage);
            if (memoryContext != null && !memoryContext.isBlank()) {
                messages.add(Map.of("role", "system", "content", memoryContext));
            }
        }

        // 2. 历史对话（排除刚刚保存的当前学生消息）
        List<ChatMessage> history = chatMessageRepository.findBySessionIdAndStudentIdOrderByCreatedAtAsc(sessionId, studentId);
        // 去掉最后一条（就是当前消息）
        if (!history.isEmpty() && "student".equals(history.get(history.size() - 1).getRole())) {
            history = history.subList(0, history.size() - 1);
        }
        // 滑动窗口：只取最近 20 条（约 10 轮对话）
        int start = Math.max(0, history.size() - 20);
        for (int i = start; i < history.size(); i++) {
            ChatMessage msg = history.get(i);
            String role = "student".equals(msg.getRole()) ? "user" : "assistant";
            messages.add(Map.of("role", role, "content", msg.getContent()));
        }

        // 3. 当前用户消息
        messages.add(Map.of("role", "user", "content", currentMessage));

        return messages;
    }

    /**
     * 引导访谈模式的 system prompt：让 AI 扮演「画像访谈官」，
     * 用轻松自然的多轮对话采集专业/目标/基础/学习偏好/薄弱点，
     * 采集充分后输出结束语并附隐藏标记 [[INTERVIEW_DONE]]，供前端检测后触发画像抽取。
     */
    private String buildInterviewSystemPrompt(Long studentId) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是 SmartMentor 的「学习画像访谈官」。你的目标是用轻松、简短、口语化的对话，")
          .append("在 3-5 轮内了解这位学生，从而为他建立个性化学习画像。\n\n");

        // 复用已有画像上下文，避免重复询问已知信息
        sb.append(buildStudentContext(studentId));

        sb.append("【访谈要点】请围绕以下方面自然地提问（不要一次性全问，每轮聚焦 1-2 点）：\n");
        sb.append("1. 专业方向与学历层次\n");
        sb.append("2. 正在学或重点关注的课程（可以是多门）、学习目标（如考研/项目实践/竞赛/通过考试）\n");
        sb.append("3. 基础水平（基础/中等/较强）\n");
        sb.append("4. 偏好的学习方式（看图表动画 / 逻辑推导 / 例题案例 / 公式速记）和资源形态（视频/文档/思维导图/实操）\n");
        sb.append("5. 自我感觉薄弱或最想攻克的模块/知识点\n\n");

        sb.append("【对话规则】\n");
        sb.append("1. 一次只问一两个问题，语气亲切，像学长学姐聊天，不要像填表\n");
        sb.append("2. 对学生的回答给一句简短回应/鼓励，再自然过渡到下一个要点\n");
        sb.append("3. 上面【学生信息】里已知的内容不要重复追问\n");
        sb.append("4. 使用 Markdown 让问题清晰，但不要输出表格或代码\n");
        sb.append("5. 全程中文\n");
        sb.append("6. 当上述要点已大致了解（通常 3-5 轮后），用一段温暖的话总结你对这位学生的理解，")
          .append("告诉他画像即将生成，并在你这条消息的最末尾单独另起一行输出标记：[[INTERVIEW_DONE]]\n");
        sb.append("7. 在没有采集到足够信息前，绝对不要输出 [[INTERVIEW_DONE]]\n");

        return contentSafetyService.withSafetyGuard(sb.toString());
    }

    /**
     * 构建 system prompt，注入学生画像和知识点上下文
     */
    private String buildSystemPrompt(Long studentId, Long pathId, String nodeId) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是 SmartMentor 智学导师——一位专业、耐心的高校课程AI伴学导师。\n\n");

        // 注入学生画像上下文
        sb.append(buildStudentContext(studentId));

        sb.append("【画像使用规则】\n");
        sb.append("1. 总体画像只用于理解学生的大方向、目标、基础和偏好；不要把总体画像等同于某一门课。\n");
        sb.append("2. 科目画像才用于判断某门课学得怎么样、哪里欠缺。学生明确提到科目/知识点时，优先使用对应科目画像。\n");
        sb.append("3. 如果学生没有说明科目，且当前没有路径节点或学习知识点，请先简短确认他想聊哪门课/哪个知识点；不要默认聚焦画像里的近期重点课程。\n");
        sb.append("4. 如果学生的问题是跨科目规划、专业方向或学习方法，就基于总体画像回答，并给出可选择的科目入口。\n\n");

        // 注入路径节点上下文
        Optional<Map<String, Object>> currentNode = resolveLearningNode(studentId, pathId, nodeId);
        if (currentNode.isPresent()) {
            Map<String, Object> node = currentNode.get();
            sb.append("【当前路径节点】\n");
            sb.append("- 节点：").append(firstNonBlank(
                    stringValue(node.get("title")),
                    stringValue(node.get("knowledgePointName")),
                    nodeId)).append("\n");
            sb.append("- 知识点：").append(firstNonBlank(
                    stringValue(node.get("knowledgePointName")),
                    stringValue(node.get("knowledgePoint")),
                    nodeId)).append("\n");
            sb.append("- 当前掌握度：").append(stringValue(node.getOrDefault("currentMastery", "未知"))).append("\n");
            sb.append("- 教学策略：").append(stringValue(node.getOrDefault("teachingStrategy", "adaptive"))).append("\n");
            if (node.get("commonErrors") != null) {
                sb.append("- 常见错误：").append(node.get("commonErrors")).append("\n");
            }
            if (node.get("focusErrors") != null) {
                sb.append("- 补救重点：").append(node.get("focusErrors")).append("\n");
            }
            sb.append("请在这个路径节点中扮演伴学导师：先判断学生卡在哪一步，再用提示、追问、小例题和订正建议推进学习；不要脱离当前知识点泛泛聊天。\n\n");
        } else if (nodeId != null) {
            String kpName = resolveKnowledgePointName(studentId, pathId, nodeId);
            sb.append("【当前学习知识点】").append(kpName).append("\n");
            sb.append("请围绕该知识点进行辅导，相关讲解和举例要紧扣这个知识点。\n\n");
        }

        // 教学原则
        sb.append("【教学原则】\n");
        sb.append("1. 用清晰、循序渐进的方式讲解，先概念后方法再练习\n");
        sb.append("2. 公式必须使用 LaTeX 且必须包裹分隔符：行内用 $...$，独立公式用 $$...$$；禁止输出裸 LaTeX，例如不要直接写 S_n = \\frac{...}{...}\n");
        sb.append("3. 引导式教学：不直接给完整答案，通过提问和提示帮助学生思考\n");
        sb.append("4. 对错误给予鼓励性反馈，指出原因并引导改正\n");
        sb.append("5. 使用 Markdown 格式（标题、列表、粗体）保持条理清晰\n");
        sb.append("6. 适时总结关键知识点和解题技巧\n");
        sb.append("7. 根据学生水平调整讲解深度和难度\n");
        sb.append("8. 全程使用中文回答\n");
        sb.append("9. 不要用字符画、ASCII Art、等宽文本或符号堆叠绘制图表、流程图或结构图，这类图学生很难看懂\n");
        sb.append("【图表能力】你可以输出真正的可视化图表，让讲解更生动直观，请主动善用：\n");
        sb.append("A. 对比、分类、参数、步骤要点等结构化信息，优先用 Markdown 表格（| 列1 | 列2 | 形式）呈现\n");
        sb.append("B. 流程、步骤、因果、层级结构、状态转换、时序交互、概念关系等，用 Mermaid 代码块绘制矢量图。");
        sb.append("语法：用 ```mermaid 包裹，graph TD/LR 画流程图与结构图，sequenceDiagram 画时序图，");
        sb.append("stateDiagram-v2 画状态图，classDiagram 画类图，erDiagram 画ER图，pie 画占比饼图，mindmap 画思维导图。\n");
        sb.append("C. Mermaid 注意事项：节点文字简短（不超过15字），中文标签放进英文双引号内如 A[\"训练集\"]，");
        sb.append("一个图聚焦一个概念，图后用一两句话解读要点；不确定语法时宁可用表格也不要写错的 Mermaid\n");
        sb.append("D. 数学公式仍用 LaTeX（$...$ / $$...$$），不要塞进 Mermaid\n");
        sb.append("E. 不滥用图表：纯文字能讲清的简单问答不必强行配图，只在图表确实能提升理解时使用\n");

        // 注入「防幻觉 + 内容安全」统一约束（满足赛题非功能性需求第3条）
        return contentSafetyService.withSafetyGuard(sb.toString());
    }

    /**
     * 从学生信息和画像中提取上下文
     */
    private String buildStudentContext(Long studentId) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("【学生信息】\n");

        Student student = studentRepository.findById(studentId).orElse(null);
        if (student != null) {
            if (student.getNickname() != null) {
                ctx.append("- 昵称：").append(student.getNickname()).append("\n");
            }
        }

        ctx.append(profileService.buildLayeredProfileContext(studentId));

        learningPathRepository.findByStudentIdAndStatusOrderByCreatedAtDesc(
                studentId, "active", PageRequest.of(0, 1)
        ).getContent().stream().findFirst().ifPresent(path -> {
            ctx.append("- 当前学习路径：").append(path.getTargetKnowledgePointName())
                    .append("，进度 ").append(path.getProgress() != null
                            ? path.getProgress().multiply(java.math.BigDecimal.valueOf(100)).intValue() : 0)
                    .append("%");
            if (path.getCurrentNodeId() != null) {
                ctx.append("，当前节点：").append(path.getCurrentNodeId());
            }
            ctx.append("\n");
        });

        diagnosticSessionRepository.findByStudentIdOrderByStartTimeDesc(
                studentId, PageRequest.of(0, 1)
        ).getContent().stream().findFirst().ifPresent(session -> {
            ctx.append("- 最近诊断：").append(session.getModule());
            if (session.getAccuracy() != null) {
                ctx.append("，正确率 ").append(session.getAccuracy().multiply(java.math.BigDecimal.valueOf(100)).intValue()).append("%");
            }
            if (session.getSuggestion() != null && !session.getSuggestion().isBlank()) {
                ctx.append("，建议：").append(compact(session.getSuggestion(), 80));
            }
            ctx.append("\n");
        });

        List<AnswerRecord> recentWrong = answerRecordRepository
                .findByStudentIdAndCreatedAtAfterOrderByCreatedAtDesc(
                        studentId, LocalDateTime.now().minusDays(30))
                .stream()
                .filter(r -> Boolean.FALSE.equals(r.getIsCorrect()))
                .limit(3)
                .collect(java.util.stream.Collectors.toList());
        if (!recentWrong.isEmpty()) {
            ctx.append("- 近期错题关注：");
            for (int i = 0; i < recentWrong.size(); i++) {
                AnswerRecord r = recentWrong.get(i);
                if (i > 0) ctx.append("；");
                ctx.append(firstNonBlank(r.getKnowledgePointName(), r.getKnowledgePointId(), "未知知识点"));
                if (r.getErrorType() != null && !r.getErrorType().isBlank()) {
                    ctx.append("/").append(r.getErrorType());
                }
            }
            ctx.append("\n");
        }

        ctx.append("\n");
        return ctx.toString();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String compact(String text, int maxLength) {
        String compacted = text.replaceAll("\\s+", " ").trim();
        return compacted.length() > maxLength ? compacted.substring(0, maxLength) + "..." : compacted;
    }

    private String mapLearningStyle(String style) {
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

    // ======================== 持久化 ========================

    private void saveAiMessage(String aiMsgId, String sessionId,
                               ChatSession session, StringBuilder content, String resourcesJson) {
        try {
            ChatMessage aiMsg = new ChatMessage();
            aiMsg.setMessageId(aiMsgId);
            aiMsg.setSessionId(sessionId);
            aiMsg.setStudentId(session.getStudentId());
            aiMsg.setRole("ai");
            aiMsg.setContent(content.toString());
            if (resourcesJson != null && !resourcesJson.isBlank()) {
                aiMsg.setResources(resourcesJson);
            }
            chatMessageRepository.save(aiMsg);

            session.setMessageCount(session.getMessageCount() + 1);
            String lastMsg = content.length() > 100 ? content.substring(0, 100) + "..." : content.toString();
            session.setLastMessage(lastMsg);
            session.setLastActiveAt(LocalDateTime.now());
            chatSessionRepository.save(session);
        } catch (Exception e) {
            log.error("保存 AI 消息失败: {}", e.getMessage());
        }
    }

    /**
     * 日常对话后，异步从最近对话静默增量更新画像（随学随新）。
     * 不阻塞 SSE；访谈模式跳过（由专门的结束抽取负责）；按消息数节流，避免每条都打 LLM。
     */
    private void maybeUpdateProfileFromChat(Long studentId, String sessionId,
                                            boolean interviewMode, ChatSession session) {
        if (interviewMode) {
            return;
        }
        try {
            int count = session.getMessageCount() != null ? session.getMessageCount() : 0;
            // 每累计约 3 轮（6 条）触发一次增量抽取
            if (count < 6 || count % 6 != 0) {
                return;
            }
            profileUpdateExecutor.submit(() -> {
                try {
                    String convo = buildRecentConversationText(studentId, sessionId, 12);
                    if (convo != null && !convo.isBlank()) {
                        conversationalProfileService.extractAndApply(studentId, convo, false);
                        detectAndApplyWeakSignals(studentId, convo);
                        // 长期记忆巩固：把最近对话压成可跨会话召回的记忆条目
                        memoryService.consolidate(studentId, sessionId, convo);
                    }
                } catch (Exception e) {
                    log.debug("对话画像静默更新失败（忽略）: {}", e.getMessage());
                }
            });
        } catch (Exception ignored) {
            // 节流判断本身出错也不能影响对话
        }
    }

    private void detectAndApplyWeakSignals(Long studentId, String convo) {
        try {
            List<WeakSignal> weakSignals = extractWeakSignals(convo);
            if (weakSignals.isEmpty()) {
                return;
            }
            for (WeakSignal signal : weakSignals) {
                applyWeakSignal(studentId, signal);
            }
        } catch (Exception e) {
            log.debug("对话薄弱点识别失败（忽略）: {}", e.getMessage());
        }
    }

    private List<WeakSignal> extractWeakSignals(String convo) throws Exception {
        String sys = "你是学习画像信号识别器。只根据最近对话识别学生明确暴露的薄弱知识点。"
                + "严格规则：1) 只有学生明确表达“不会/没懂/卡住/总错/混淆/做不出来/解释后仍困惑”等困惑或反复错误时才输出；"
                + "2) 不要把导师主动讲过的主题、学生的一般提问或资源请求当成薄弱点；"
                + "3) module 必须尽量是课程/模块名，例如“人工智能基础”“Java Web 开发”“数字电路基础”，无法确定就用学生原话里的科目/主题；"
                + "4) errorType 用简短中文概括错误类型，例如“概念混淆”“公式不会用”“步骤卡顿”“代码实现错误”；"
                + "5) confidence 为 0 到 1；没有明确薄弱点时返回 {\"weakPoints\":[]}；"
                + "6) 只返回严格 JSON：{\"weakPoints\":[{\"knowledgePointName\":\"...\",\"module\":\"...\",\"errorType\":\"...\",\"confidence\":0.0}]}。";
        String resp = llmService.chatJsonSync(convo, sys, 0.1);
        Map<String, Object> data = objectMapper.readValue(resp, new TypeReference<Map<String, Object>>() {});
        Object raw = data.get("weakPoints");
        if (!(raw instanceof List)) {
            return Collections.emptyList();
        }
        List<WeakSignal> result = new ArrayList<>();
        for (Object item : (List<?>) raw) {
            if (!(item instanceof Map)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) item;
            String knowledgePointName = stringValue(map.get("knowledgePointName")).trim();
            String module = stringValue(map.get("module")).trim();
            String errorType = stringValue(map.get("errorType")).trim();
            double confidence = doubleValue(map.get("confidence"));
            if (confidence >= 0.6 && !knowledgePointName.isBlank()) {
                result.add(new WeakSignal(knowledgePointName, module, errorType, confidence));
            }
        }
        return result;
    }

    private void applyWeakSignal(Long studentId, WeakSignal signal) {
        String module = signal.module;
        KnowledgeGraphService.KnowledgeNode node = null;
        if (module == null || module.isBlank()) {
            node = findBestKnowledgeNodeAcrossSubjects(signal.knowledgePointName);
            if (node != null) {
                module = node.getModule();
            }
        }
        module = firstNonBlank(module, inferModuleFromProfile(studentId), "课程知识点");
        if (node == null) {
            node = findBestKnowledgeNode(module, signal.knowledgePointName);
        }
        if (node != null) {
            masteryUpdateService.recordKnowledgePointMastery(
                    studentId, node.getId(), knowledgeGraphService.normalizeModuleName(module), 0.30, "chat");
        }
        String weakTopic = node != null ? node.getName() : signal.knowledgePointName;
        String priority = knowledgeGraphService.normalizeModuleName(module);
        if (!weakTopic.equals(priority)) {
            priority = priority + " · " + weakTopic;
        }
        profileService.appendWeakModulePriority(studentId, priority);
        profileService.appendErrorPattern(studentId,
                firstNonBlank(signal.errorType, "对话暴露薄弱"), weakTopic);
        storeActionCard(studentId, module, weakTopic, signal.errorType);
    }

    private KnowledgeGraphService.KnowledgeNode findBestKnowledgeNode(String module, String knowledgePointName) {
        if (module == null || module.isBlank() || knowledgePointName == null || knowledgePointName.isBlank()) {
            return null;
        }
        List<KnowledgeGraphService.KnowledgeNode> nodes = knowledgeGraphService.getNodesByModule(module);
        if (nodes.isEmpty()) {
            return null;
        }
        String target = normalizeForMatch(knowledgePointName);
        KnowledgeGraphService.KnowledgeNode best = null;
        int bestScore = 0;
        for (KnowledgeGraphService.KnowledgeNode node : nodes) {
            String name = normalizeForMatch(node.getName());
            int score = matchScore(name, target);
            if (score > bestScore) {
                bestScore = score;
                best = node;
            }
        }
        return bestScore >= 2 ? best : null;
    }

    private KnowledgeGraphService.KnowledgeNode findBestKnowledgeNodeAcrossSubjects(String knowledgePointName) {
        if (knowledgePointName == null || knowledgePointName.isBlank()) {
            return null;
        }
        String target = normalizeForMatch(knowledgePointName);
        KnowledgeGraphService.KnowledgeNode best = null;
        int bestScore = 0;
        for (String module : knowledgeGraphService.getAllModules()) {
            for (KnowledgeGraphService.KnowledgeNode node : knowledgeGraphService.getNodesByModule(module)) {
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

    private String inferModuleFromProfile(Long studentId) {
        return studentProfileRepository.findByStudentId(studentId)
                .map(StudentProfile::getCurrentCourse)
                .orElse(null);
    }

    private void storeActionCard(Long studentId, String module, String knowledgePointName, String errorType) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("module", knowledgeGraphService.normalizeModuleName(firstNonBlank(module, knowledgePointName)));
        card.put("knowledgePointName", knowledgePointName);
        card.put("reason", firstNonBlank(errorType, "最近对话暴露了这个薄弱点"));
        try {
            redisUtil.set(ACTION_CARD_KEY_PREFIX + studentId, card, 1, TimeUnit.DAYS);
        } catch (Exception e) {
            log.debug("写入对话行动建议卡失败（忽略）: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> consumeActionCard(Long studentId) {
        String key = ACTION_CARD_KEY_PREFIX + studentId;
        try {
            Object raw = redisUtil.get(key);
            if (raw == null) {
                return null;
            }
            redisUtil.delete(key);
            if (raw instanceof Map) {
                return new LinkedHashMap<>((Map<String, Object>) raw);
            }
            if (raw instanceof String && !((String) raw).isBlank()) {
                return objectMapper.readValue((String) raw, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.debug("读取对话行动建议卡失败（忽略）: {}", e.getMessage());
        }
        return null;
    }

    private double doubleValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return value == null ? 0.0 : Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static class WeakSignal {
        private final String knowledgePointName;
        private final String module;
        private final String errorType;
        @SuppressWarnings("unused")
        private final double confidence;

        private WeakSignal(String knowledgePointName, String module, String errorType, double confidence) {
            this.knowledgePointName = knowledgePointName;
            this.module = module;
            this.errorType = errorType;
            this.confidence = confidence;
        }
    }

    /** 取最近 maxMessages 条对话拼成「学生/导师」文本，供画像抽取 */
    private String buildRecentConversationText(Long studentId, String sessionId, int maxMessages) {
        List<ChatMessage> history = chatMessageRepository.findBySessionIdAndStudentIdOrderByCreatedAtAsc(sessionId, studentId);
        if (history.isEmpty()) {
            return "";
        }
        int start = Math.max(0, history.size() - maxMessages);
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < history.size(); i++) {
            ChatMessage m = history.get(i);
            String role = "student".equals(m.getRole()) ? "学生" : "导师";
            sb.append(role).append("：").append(compact(m.getContent(), 200)).append("\n");
        }
        return sb.toString().trim();
    }

    // ======================== 历史查询 ========================

    public Map<String, Object> getHistory(Long studentId, String sessionId, Long pathId,
                                          int page, int size) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (sessionId != null && !sessionId.isEmpty()) {
            ChatSession session = chatSessionRepository.findBySessionId(sessionId).orElse(null);
            if (session == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "会话不存在");
            }
            assertSessionOwner(session, studentId);

            List<ChatMessage> messages = chatMessageRepository.findBySessionIdAndStudentIdOrderByCreatedAtAsc(sessionId, studentId);

            List<Map<String, Object>> msgList = new ArrayList<>();
            for (ChatMessage msg : messages) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("messageId", msg.getMessageId());
                m.put("role", msg.getRole());
                m.put("content", msg.getContent());
                m.put("timestamp", msg.getCreatedAt());
                List<Map<String, Object>> res = deserializeResources(msg.getResources());
                if (!res.isEmpty()) {
                    m.put("resources", res);
                }
                msgList.add(m);
            }

            result.put("sessionId", sessionId);
            result.put("title", session.getTitle());
            Map<String, Object> relatedPath = new LinkedHashMap<>();
            relatedPath.put("pathId", session.getPathId());
            relatedPath.put("targetKnowledgePoint", session.getKnowledgePointName());
            relatedPath.put("currentNode", session.getKnowledgePointName());
            result.put("relatedPath", relatedPath);
            result.put("messages", msgList);
            result.put("totalMessages", msgList.size());
            result.put("createdAt", session.getCreatedAt());
            result.put("lastActiveAt", session.getLastActiveAt());
        } else {
            Pageable pageable = PageRequest.of(page, size);
            Page<ChatSession> sessionPage;

            if (pathId != null) {
                sessionPage = chatSessionRepository.findByStudentIdAndPathIdOrderByLastActiveAtDesc(
                        studentId, pathId, pageable);
            } else {
                sessionPage = chatSessionRepository.findByStudentIdOrderByLastActiveAtDesc(
                        studentId, pageable);
            }

            List<Map<String, Object>> sessionList = new ArrayList<>();
            for (ChatSession s : sessionPage.getContent()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("sessionId", s.getSessionId());
                item.put("title", s.getTitle());
                Map<String, Object> relatedPath = new LinkedHashMap<>();
                relatedPath.put("pathId", s.getPathId());
                relatedPath.put("knowledgePointName", s.getKnowledgePointName());
                item.put("relatedPath", relatedPath);
                item.put("messageCount", s.getMessageCount());
                item.put("lastMessage", s.getLastMessage());
                item.put("createdAt", s.getCreatedAt());
                item.put("lastActiveAt", s.getLastActiveAt());
                sessionList.add(item);
            }

            result.put("sessions", sessionList);
            result.put("total", sessionPage.getTotalElements());
            result.put("page", page);
            result.put("size", size);
            result.put("totalPages", sessionPage.getTotalPages());
        }

        return result;
    }

    private void assertSessionOwner(ChatSession session, Long studentId) {
        if (!Objects.equals(session.getStudentId(), studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权访问该会话");
        }
    }

    // ======================== 工具方法 ========================

    private static final String[] RESOURCE_INTENT_KEYWORDS = {
            "视频", "资料", "教程", "资源", "推荐", "课程视频", "学习资料",
            "找一下", "找个", "找点", "有没有", "哪里学", "哪里看", "看看", "b站", "B站", "慕课", "公开课"
    };

    /**
     * 判断学生消息是否在索要学习资源/视频
     */
    private boolean wantsLearningResource(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        for (String kw : RESOURCE_INTENT_KEYWORDS) {
            if (message.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    /** 不能作为检索主题的无意义词 */
    private static final Set<String> TOPIC_STOPWORDS = new java.util.HashSet<>(Arrays.asList(
            "重新", "再", "又", "这个", "那个", "它", "他", "她", "视频", "资料", "教程", "资源",
            "推荐", "找", "学习", "一下", "看看", "看", "课程", "内容", "知识", "东西", "什么",
            "怎么", "如何", "相关", "一些", "几个", "继续", "上面", "刚才", "刚刚"
    ));

    /**
     * 解析学生想检索的学习主题：结合最近对话用 AI 抽取，失败回退知识点/启发式；
     * 解析不到有效主题则返回 null（宁可不推卡片，也不推无关内容）。
     */
    private String resolveResourceQuery(Long studentId, String sessionId, String message, String fallbackKnowledgePoint) {
        boolean hasKnowledgePoint = fallbackKnowledgePoint != null && !fallbackKnowledgePoint.isBlank();
        // 学生最新消息是否自带新主题：剥离“重新/找一下/视频”等指令词后仍有实质内容
        boolean messageHasOwnTopic = isValidTopic(extractResourceQuery(message, null));

        String recentContext = buildRecentTopicContext(studentId, sessionId);
        String aiTopic = null;
        try {
            String sys = "你是学习视频检索词抽取器。根据【当前知识点】【最近对话】和【学生最新消息】，"
                    + "判断学生此刻想检索的‘学习主题’（具体的课程/知识点/技术名词）。规则："
                    + "1) 只返回严格JSON：{\"query\":\"...\"}；"
                    + "2) query 必须是具体的学科主题，如‘机器学习 训练集 测试集’、‘HTTP协议’、‘数字电路 触发器’，长度2-15字；"
                    + "3) query 严禁包含‘重新/再/又/继续/这个/那个/帮我找/找一下/推荐/视频/教程/资料/资源’等指令或填充词，只保留学科名词；"
                    + "4) 就近优先：当前讨论的主题以【最近一轮】对话为准。若【最近对话】里有多个主题，"
                    + "只取标有【最近一轮】的那条所讨论的主题，绝不要用更早轮次的旧主题；"
                    + "5) 若最新消息只是‘重新找一下/再来几个/换一批’等指令而没有给出新主题，"
                    + "必须沿用【最近一轮】正在讨论的主题（其次才是【当前知识点】），绝不能把指令词当成主题；"
                    + "6) 实在无法确定主题时 query 返回空字符串。";
            String user = "【当前知识点】" + (hasKnowledgePoint ? fallbackKnowledgePoint : "（无）")
                    + "\n【最近对话】\n" + (recentContext.isBlank() ? "（无）" : recentContext)
                    + "\n【学生最新消息】" + message;
            String resp = llmService.chatJsonSync(user, sys, 0.1);
            Map<String, Object> data = objectMapper.readValue(resp,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            String q = stringValue(data.get("query")).trim();
            if (isValidTopic(q)) {
                aiTopic = q;
            }
        } catch (Exception e) {
            log.debug("AI 检索词抽取失败，使用回退: {}", e.getMessage());
        }

        // 优先级：
        // 1) 学生消息自带主题 且 AI 抽到有效主题 → 用 AI 主题（学生确实在换新主题）
        if (messageHasOwnTopic && aiTopic != null) {
            return aiTopic;
        }
        // 2) 绑定了知识点 → 锚定到当前知识点（指令型消息如“重新找一下”走这里，避免用指令词检索）
        if (hasKnowledgePoint) {
            return fallbackKnowledgePoint;
        }
        // 3) 无知识点但 AI 从上下文推断出了主题 → 用 AI 主题
        if (aiTopic != null) {
            return aiTopic;
        }
        // 4) 启发式兜底（当前消息）
        String h = extractResourceQuery(message, null);
        if (isValidTopic(h)) {
            return h;
        }
        // 5) 从最近对话里挖掘上一个有效主题（不依赖 AI）：
        //    指令型消息如“再来几个/换一批”在前几轮已出现过主题，确定性地复用它，
        //    消除因 AI 抽取偶发失败导致的“有概率不推荐”。
        String recentTopic = mineRecentTopic(studentId, sessionId);
        return isValidTopic(recentTopic) ? recentTopic : null;
    }

    /**
     * 从最近对话里挖掘当前正在讨论的主题（不依赖 AI），强就近关联：
     * 只回看最近一轮（最多 4 条，新→旧），优先取学生倒数第二条消息里的主题，
     * 其次取上一条 AI 回复开头的标题/知识点。窗口刻意收窄，避免捞到几轮前的旧主题
     * 导致“推荐卡片与当前主题无关”。
     */
    private String mineRecentTopic(Long studentId, String sessionId) {
        try {
            List<ChatMessage> history = chatMessageRepository.findBySessionIdAndStudentIdOrderByCreatedAtAsc(sessionId, studentId);
            // 末条通常是刚保存的当前消息（指令型），从它之前开始回看，窗口仅 1 轮
            int end = history.size() - 1;
            int floor = Math.max(0, end - 4);
            // 1) 先找最近的一条“学生历史消息”里的实质主题
            for (int i = end - 1; i >= floor; i--) {
                ChatMessage m = history.get(i);
                if (!"student".equals(m.getRole())) {
                    continue;
                }
                String t = extractResourceQuery(m.getContent(), null);
                if (isValidTopic(t)) {
                    return t;
                }
                break; // 只认最近一条学生消息，不再向更早的轮次回溯
            }
            // 2) 退而取上一条 AI 回复开头的标题/知识点（代表“当前正在讲什么”）
            for (int i = end - 1; i >= floor; i--) {
                ChatMessage m = history.get(i);
                if ("student".equals(m.getRole())) {
                    continue;
                }
                String t = extractTopicFromAiReply(m.getContent());
                if (isValidTopic(t)) {
                    return t;
                }
                break;
            }
        } catch (Exception e) {
            log.debug("挖掘最近主题失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 从 AI 回复里提取“正在讲解的主题”：取首个 Markdown 标题或首句的核心名词短语。
     */
    private String extractTopicFromAiReply(String reply) {
        if (reply == null || reply.isBlank()) {
            return null;
        }
        for (String line : reply.split("\\r?\\n")) {
            String s = line.trim();
            if (s.isEmpty()) {
                continue;
            }
            // Markdown 标题行：# / ## / ### 后的文字最能代表主题
            if (s.startsWith("#")) {
                String title = s.replaceAll("^#+\\s*", "").replaceAll("[*`_]", "").trim();
                String t = extractResourceQuery(title, null);
                if (isValidTopic(t)) {
                    return t;
                }
            }
            break; // 仅看正文首个非空行，避免误取后文无关段落
        }
        return null;
    }

    /**
     * 校验主题是否有效：长度达标且不是无意义停用词
     */
    private boolean isValidTopic(String topic) {
        if (topic == null) {
            return false;
        }
        String t = topic.trim();
        if (t.length() < 2) {
            return false;
        }
        // 去掉空格后若整体就是停用词，判定无效
        String compact = t.replaceAll("\\s+", "");
        if (TOPIC_STOPWORDS.contains(compact)) {
            return false;
        }
        // 若由多个词组成，至少要有一个非停用词且长度>=2
        for (String part : t.split("\\s+")) {
            if (part.length() >= 2 && !TOPIC_STOPWORDS.contains(part)) {
                return true;
            }
        }
        return !TOPIC_STOPWORDS.contains(compact) && compact.length() >= 2;
    }

    /**
     * 取最近若干条对话内容作为主题解析上下文（用于解析"重新找/再来一个"等指代）
     */
    private String buildRecentTopicContext(Long studentId, String sessionId) {
        try {
            List<ChatMessage> history = chatMessageRepository.findBySessionIdAndStudentIdOrderByCreatedAtAsc(sessionId, studentId);
            // 末条通常是刚保存的当前消息（指令型），主题解析靠它之前的对话，故排除末条
            int upper = Math.max(0, history.size() - 1);
            int start = Math.max(0, upper - 4); // 仅最近 2 轮，强就近关联
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < upper; i++) {
                ChatMessage m = history.get(i);
                boolean isStudent = "student".equals(m.getRole());
                String role = isStudent ? "学生" : "导师";
                // 放宽截断：学生消息通常短、全留；导师回复取首行标题+正文前 160 字，
                // 保证“正在讲解的主题”不被 80 字截断丢掉
                String content = isStudent ? compact(m.getContent(), 120)
                                           : summarizeAiReply(m.getContent(), 160);
                String tag = (i == upper - 1) ? "【最近一轮】" : "";
                sb.append(tag).append(role).append("：").append(content).append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 压缩 AI 回复用于主题上下文：优先保留首个 Markdown 标题（最能代表主题），
     * 再附正文摘要，避免长篇回复把主题词挤出截断窗口。
     */
    private String summarizeAiReply(String reply, int maxLength) {
        if (reply == null || reply.isBlank()) {
            return "";
        }
        String heading = "";
        for (String line : reply.split("\\r?\\n")) {
            String s = line.trim();
            if (s.startsWith("#")) {
                heading = s.replaceAll("^#+\\s*", "").replaceAll("[*`_]", "").trim();
                break;
            }
        }
        String body = compact(reply, maxLength);
        return heading.isBlank() ? body : "（主题：" + heading + "）" + body;
    }

    /**
     * 从学生消息中提取检索主题：去掉口语化填充词；提取不到时回退到当前知识点
     */
    private String extractResourceQuery(String message, String fallbackKnowledgePoint) {
        if (message == null) {
            message = "";
        }
        String query = message;
        String[] fillers = {
                "帮我", "请", "我想", "我要", "想", "找一下", "找个", "找点", "找", "推荐", "一下",
                "有没有", "给我", "关于", "的视频", "的资料", "的教程", "的资源", "学习资料",
                "课程视频", "视频", "资料", "教程", "资源", "看看", "看", "b站", "B站", "慕课",
                "公开课", "讲解", "相关", "一些", "几个", "学习", "怎么", "如何", "吗", "呢", "啊",
                "重新", "再", "又", "继续", "?", "？", "，", ",", "。"
        };
        for (String f : fillers) {
            query = query.replace(f, " ");
        }
        query = query.replaceAll("\\s+", " ").trim();
        if (query.length() < 2) {
            return (fallbackKnowledgePoint != null && !fallbackKnowledgePoint.isBlank())
                    ? fallbackKnowledgePoint : null;
        }
        return query;
    }

    /**
     * 解析检索主题：优先用 AI 从消息中抽取干净的主题词，失败回退到启发式剥离。
     */
    private String resolveResourceQuery(String message, String fallbackKnowledgePoint) {
        try {
            String sys = "你是学习视频检索词抽取器。从学生的一句话中提取最适合用于检索“学习/教学视频”的主题词"
                    + "（课程名、知识点或技术名词），只返回严格 JSON：{\"query\":\"...\"}。"
                    + "要求：query 简洁（2-15 字），只保留学科主题，去掉“帮我找/推荐/视频/教程/资料/资源”等词；"
                    + "若学生并非在找学习资料，query 返回空字符串。";
            String resp = llmService.chatJsonSync(message, sys, 0.1);
            Map<String, Object> data = objectMapper.readValue(resp,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            String q = stringValue(data.get("query")).trim();
            if (!q.isBlank()) {
                return q;
            }
        } catch (Exception e) {
            log.debug("AI 检索词抽取失败，使用启发式: {}", e.getMessage());
        }
        return extractResourceQuery(message, fallbackKnowledgePoint);
    }

    private String serializeResources(List<Map<String, Object>> resources) {
        if (resources == null || resources.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(resources);
        } catch (Exception e) {
            return null;
        }
    }

    private List<Map<String, Object>> deserializeResources(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String resolveKnowledgePointName(Long studentId, Long pathId, String nodeId) {
        if (nodeId == null) return null;
        Optional<Map<String, Object>> learningNode = resolveLearningNode(studentId, pathId, nodeId);
        if (learningNode.isPresent()) {
            Map<String, Object> node = learningNode.get();
            return firstNonBlank(
                    stringValue(node.get("knowledgePointName")),
                    stringValue(node.get("knowledgePoint")),
                    stringValue(node.get("title")));
        }
        KnowledgeGraphService.KnowledgeNode node = knowledgeGraphService.getNode(nodeId);
        return node != null ? node.getName() : "课程知识点";
    }

    private Optional<Map<String, Object>> resolveLearningNode(Long studentId, Long pathId, String nodeId) {
        if (pathId == null || nodeId == null || nodeId.isBlank()) {
            return Optional.empty();
        }
        return learningPathRepository.findById(pathId)
                .flatMap(path -> {
                    if (!path.getStudentId().equals(studentId)) {
                        return Optional.empty();
                    }
                    try {
                        List<Map<String, Object>> nodes = objectMapper.readValue(path.getNodes(),
                                new TypeReference<List<Map<String, Object>>>() {});
                        return nodes.stream()
                                .filter(node -> nodeId.equals(String.valueOf(node.get("nodeId"))))
                                .findFirst();
                    } catch (Exception e) {
                        log.warn("解析学习路径节点失败: pathId={}, nodeId={}", pathId, nodeId);
                        return Optional.empty();
                    }
                });
    }
}
