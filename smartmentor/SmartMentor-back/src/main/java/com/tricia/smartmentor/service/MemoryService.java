package com.tricia.smartmentor.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tricia.smartmentor.entity.StudentMemory;
import com.tricia.smartmentor.repository.StudentMemoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/**
 * 长期记忆服务（情景记忆）。设计见 docs/MEMORY_DESIGN.md。
 * <p>
 * 两个入口：
 * <ul>
 *   <li>{@link #recall} —— 读取侧，在对话 prompt 组装时调用。受总开关 + 超时 + 异常降级三重保护，
 *       任何异常/超时都返回空，绝不推迟 SSE 首包。</li>
 *   <li>{@link #consolidate} —— 写入侧，挂在 ChatService 已有的异步写回循环里。
 *       用 LLM 把最近对话压成一句话记忆，content_hash 去重后入库。</li>
 * </ul>
 * 当前为<b>关键词召回版</b>：{@link EmbeddingClient} 无实现 Bean 时召回走 n-gram 关键词匹配；
 * 接入向量化后自动改用余弦相似度，上层无需改动。
 */
@Slf4j
@Service
public class MemoryService {

    private final StudentMemoryRepository memoryRepository;
    private final LlmService llmService;
    private final ObjectMapper objectMapper;
    /** 关键词版无 EmbeddingClient Bean，注入为 null，召回降级关键词匹配。 */
    @Autowired(required = false)
    private EmbeddingClient embeddingClient;

    private final boolean memoryEnabled;
    private final boolean embeddingEnabled;
    private final long recallTimeoutMs;
    private final int recallTopK;
    private final double similarityThreshold;

    /**
     * 召回超时执行器：embed/DB 慢时由它兜底超时，避免阻塞首包。
     * <b>有界</b>线程数与队列：卡顿堆积时新召回直接被拒（降级跳过），
     * 绝不无限增长 memory-recall 线程或排队拖慢对话。
     */
    private final ExecutorService recallExecutor =
            new ThreadPoolExecutor(2, 4, 60L, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(16),
                    r -> {
                        Thread t = new Thread(r, "memory-recall");
                        t.setDaemon(true);
                        return t;
                    },
                    new ThreadPoolExecutor.AbortPolicy());

    public MemoryService(StudentMemoryRepository memoryRepository,
                         LlmService llmService,
                         ObjectMapper objectMapper,
                         @Value("${smartmentor.memory.enabled:true}") boolean memoryEnabled,
                         @Value("${smartmentor.memory.embedding-enabled:true}") boolean embeddingEnabled,
                         @Value("${smartmentor.memory.recall-timeout-ms:300}") long recallTimeoutMs,
                         @Value("${smartmentor.memory.recall-top-k:4}") int recallTopK,
                         @Value("${smartmentor.memory.similarity-threshold:0.0}") double similarityThreshold) {
        this.memoryRepository = memoryRepository;
        this.llmService = llmService;
        this.objectMapper = objectMapper;
        this.memoryEnabled = memoryEnabled;
        this.embeddingEnabled = embeddingEnabled;
        this.recallTimeoutMs = recallTimeoutMs;
        this.recallTopK = recallTopK;
        this.similarityThreshold = similarityThreshold;
    }

    // ======================== 读取侧：召回 ========================

    /**
     * 召回与 query 最相关的若干条记忆，拼成可注入 system prompt 的【相关记忆】文本段。
     * 受总开关 + 超时 + 异常降级保护：关闭/无记忆/超时/异常一律返回空串（当作无相关记忆）。
     *
     * @return 形如 "【关于这位学生的长期记忆】\n- ...\n" 的文本；无可注入内容时返回 ""
     */
    public String recall(Long studentId, String query) {
        if (!memoryEnabled || studentId == null || query == null || query.isBlank()) {
            return "";
        }
        Future<String> future = null;
        try {
            // 整段召回（含可能的远程 embed + DB 查询）包在超时里，超时即放弃
            future = recallExecutor.submit(() -> doRecall(studentId, query));
            return future.get(recallTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            // 超时：中断底层任务（embed/DB），避免它在调用方走后继续占用线程和远程配额。
            // ponytail: cancel(true) 对阻塞中的 JDBC 读无效（不响应中断），真正的堆积兜底是有界线程池；
            //           若将来 embed 走 HTTP，可在客户端侧设更短的读超时进一步收紧。
            future.cancel(true);
            log.debug("记忆召回超时({}ms)，本轮跳过", recallTimeoutMs);
            return "";
        } catch (RejectedExecutionException e) {
            // 线程池/队列已满（卡顿堆积）：直接降级跳过，不拖慢对话
            log.debug("记忆召回线程池繁忙，本轮跳过");
            return "";
        } catch (Exception e) {
            if (future != null) {
                future.cancel(true);
            }
            log.debug("记忆召回失败（忽略）: {}", e.getMessage());
            return "";
        }
    }

    private String doRecall(Long studentId, String query) {
        List<StudentMemory> all = memoryRepository.findByStudentId(studentId);
        if (all.isEmpty()) {
            return "";
        }

        // 查询向量：embedding 可用且开启时才取，否则全程走关键词
        float[] queryVec = (embeddingEnabled && embeddingClient != null)
                ? safeEmbed(query) : null;
        String queryModel = (queryVec != null && embeddingClient != null)
                ? embeddingClient.modelName() : null;

        List<Scored> scored = new ArrayList<>();
        for (StudentMemory m : all) {
            double score;
            float[] memVec = parseEmbedding(m);
            // 仅当查询向量与记忆向量同模型同维度时才算余弦，否则降级关键词
            boolean comparable = queryVec != null && memVec != null
                    && Objects.equals(queryModel, m.getEmbeddingModel())
                    && memVec.length == queryVec.length;
            if (comparable) {
                score = cosine(queryVec, memVec);
            } else {
                score = keywordScore(query, m.getContent());
            }
            if (score > similarityThreshold) {
                scored.add(new Scored(m, score));
            }
        }
        if (scored.isEmpty()) {
            return "";
        }
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        StringBuilder sb = new StringBuilder("【关于这位学生的长期记忆（来自历史对话，供你参考，不要逐条复述）】\n");
        int n = Math.min(recallTopK, scored.size());
        for (int i = 0; i < n; i++) {
            sb.append("- ").append(scored.get(i).memory.getContent()).append("\n");
        }
        return sb.toString();
    }

    private float[] safeEmbed(String text) {
        try {
            return embeddingClient.embed(text);
        } catch (Exception e) {
            log.debug("embedding 调用失败，降级关键词: {}", e.getMessage());
            return null;
        }
    }

    private float[] parseEmbedding(StudentMemory m) {
        if (m.getEmbedding() == null || m.getEmbedding().isBlank()) {
            return null;
        }
        try {
            List<Double> list = objectMapper.readValue(m.getEmbedding(),
                    new TypeReference<List<Double>>() {});
            float[] vec = new float[list.size()];
            for (int i = 0; i < list.size(); i++) {
                vec[i] = list.get(i).floatValue();
            }
            return vec;
        } catch (Exception e) {
            return null;
        }
    }

    // ======================== 写入侧：巩固 ========================

    /**
     * 把最近对话压成 0~N 条一句话记忆并入库。<b>调用方须保证在异步线程上执行</b>
     * （ChatService 复用其 profileUpdateExecutor 调用），本方法内含 LLM 同步调用。
     *
     * @param conversationText 最近对话文本（"学生：.../导师：..."），由调用方拼好
     */
    public void consolidate(Long studentId, String sessionId, String conversationText) {
        if (!memoryEnabled || studentId == null
                || conversationText == null || conversationText.isBlank()) {
            return;
        }
        try {
            List<Map<String, String>> items = extractMemoryItems(conversationText);
            for (Map<String, String> item : items) {
                String content = item.getOrDefault("content", "").trim();
                String type = normalizeType(item.get("type"));
                if (content.length() < 4 || content.length() > 500) {
                    continue;
                }
                String hash = sha256(content);
                if (memoryRepository.existsByStudentIdAndContentHash(studentId, hash)) {
                    continue;
                }
                StudentMemory mem = new StudentMemory();
                mem.setStudentId(studentId);
                mem.setType(type);
                mem.setContent(content);
                mem.setContentHash(hash);
                mem.setSourceSession(sessionId);
                // 关键词版：embedding 留空。接入向量化后在此 embed 并填 provider/model/dim。
                if (embeddingEnabled && embeddingClient != null) {
                    fillEmbedding(mem, content);
                }
                memoryRepository.save(mem);
            }
        } catch (Exception e) {
            log.debug("记忆巩固失败（忽略）: {}", e.getMessage());
        }
    }

    private void fillEmbedding(StudentMemory mem, String content) {
        try {
            float[] vec = embeddingClient.embed(content);
            if (vec == null || vec.length == 0) {
                return;
            }
            mem.setEmbedding(objectMapper.writeValueAsString(vec));
            mem.setEmbeddingProvider(embeddingClient.providerName());
            mem.setEmbeddingModel(embeddingClient.modelName());
            mem.setEmbeddingDim(vec.length);
        } catch (Exception e) {
            log.debug("记忆向量化失败，留空走关键词: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> extractMemoryItems(String conversationText) throws Exception {
        String sys = "你是学习对话的长期记忆提炼器。从最近对话中提炼对‘长期个性化辅导’有价值、"
                + "且较稳定的事实，每条压成一句话。严格规则："
                + "1) 只提炼跨会话仍有用的稳定信息：学生的偏好、目标、反复暴露的薄弱点、已确认掌握的能力、个人背景；"
                + "2) 不要提炼一次性的闲聊、寒暄、本轮临时问答、导师讲过的知识本身；"
                + "3) 每条 content 为简洁中文陈述句，不超过60字，含具体主题（如‘对反向传播链式法则反复混淆’）；"
                + "4) type 取值：fact(背景事实)/preference(学习偏好)/weakness(薄弱点)/goal(目标)；"
                + "5) 没有值得长期记住的内容时返回 {\"memories\":[]}；最多 3 条；"
                + "6) 只返回严格 JSON：{\"memories\":[{\"type\":\"...\",\"content\":\"...\"}]}。";
        String resp = llmService.chatJsonSync(conversationText, sys, 0.2);
        Map<String, Object> data = objectMapper.readValue(resp,
                new TypeReference<Map<String, Object>>() {});
        Object raw = data.get("memories");
        if (!(raw instanceof List)) {
            return Collections.emptyList();
        }
        List<Map<String, String>> result = new ArrayList<>();
        for (Object o : (List<?>) raw) {
            if (o instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) o;
                Map<String, String> item = new HashMap<>();
                item.put("type", map.get("type") == null ? "" : String.valueOf(map.get("type")));
                item.put("content", map.get("content") == null ? "" : String.valueOf(map.get("content")));
                result.add(item);
            }
        }
        return result;
    }

    private String normalizeType(String type) {
        if (type == null) {
            return "fact";
        }
        String t = type.trim().toLowerCase(Locale.ROOT);
        switch (t) {
            case "preference":
            case "weakness":
            case "goal":
            case "fact":
                return t;
            default:
                return "fact";
        }
    }

    // ======================== 相似度 ========================

    /** 余弦相似度。要求等长非零向量；退化情形返回 0。 */
    static double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) {
            return 0.0;
        }
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) {
            return 0.0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    /**
     * 关键词 n-gram 相似度（embedding 不可用时的兜底）：归一化后按 2-gram 命中数打分，
     * 再除以查询长度做归一，落到 (0,1] 量级，与余弦阈值大致可比。
     */
    static double keywordScore(String query, String content) {
        String q = normalize(query);
        String c = normalize(content);
        if (q.length() < 2 || c.isEmpty()) {
            return 0.0;
        }
        if (c.contains(q) || q.contains(c)) {
            return 1.0;
        }
        int hit = 0, total = 0;
        for (int i = 0; i < q.length() - 1; i++) {
            total++;
            if (c.contains(q.substring(i, i + 2))) {
                hit++;
            }
        }
        return total == 0 ? 0.0 : (double) hit / total;
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\p{Punct}，。、“”‘’（）()【】\\[\\]：:；;·-]+", "");
    }

    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return Integer.toHexString(text.hashCode());
        }
    }

    private static class Scored {
        final StudentMemory memory;
        final double score;
        Scored(StudentMemory memory, double score) {
            this.memory = memory;
            this.score = score;
        }
    }
}
