package com.tricia.smartmentor.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BilibiliVideoService {

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final String SEARCH_API = "https://api.bilibili.com/x/web-interface/search/type";
    private static final String SPI_API = "https://api.bilibili.com/x/frontend/finger/spi";
    private static final String HOME_URL = "https://www.bilibili.com";

    private final boolean enabled;
    private final List<String> preferredAuthors;
    private final LlmService llmService;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;
    /** B 站风控所需 Cookie（buvid3 等），首次搜索时惰性获取 */
    private volatile String cookieHeader;

    public BilibiliVideoService(@Value("${smartmentor.bilibili.enabled:true}") boolean enabled,
                                @Value("${smartmentor.bilibili.preferred-authors:中国大学MOOC,学堂在线,北京大学,清华大学,浙江大学,慕课网}") String preferredAuthorsCsv,
                                LlmService llmService,
                                ObjectMapper objectMapper) {
        this.enabled = enabled;
        this.preferredAuthors = parseAuthors(preferredAuthorsCsv);
        this.llmService = llmService;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .writeTimeout(8, TimeUnit.SECONDS)
                .build();
    }

    private static List<String> parseAuthors(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split("[,，]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    @PreDestroy
    public void destroy() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }

    /**
     * 惰性获取 B 站风控所需的 buvid3 Cookie。
     * 无此 Cookie 时搜索接口会返回 code=-412（请求被拦截），data.result 为空。
     */
    private String ensureCookie() {
        if (cookieHeader != null) {
            return cookieHeader;
        }
        synchronized (this) {
            if (cookieHeader != null) {
                return cookieHeader;
            }
            String cookie = fetchCookieFromSpi();
            if (isBlank(cookie)) {
                cookie = fetchCookieFromHome();
            }
            if (isBlank(cookie)) {
                // 兜底：生成一个伪 buvid3，多数情况下也能让搜索接口放行
                cookie = "buvid3=" + java.util.UUID.randomUUID().toString().toUpperCase() + "infoc";
            }
            cookieHeader = cookie;
            log.info("Bilibili cookie 初始化完成");
            return cookieHeader;
        }
    }

    private String fetchCookieFromSpi() {
        Request request = new Request.Builder()
                .url(SPI_API)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .addHeader("Referer", HOME_URL)
                .get()
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return null;
            }
            JsonNode data = objectMapper.readTree(response.body().string()).path("data");
            String b3 = data.path("b_3").asText("");
            String b4 = data.path("b_4").asText("");
            if (isBlank(b3)) {
                return null;
            }
            StringBuilder sb = new StringBuilder("buvid3=").append(b3);
            if (!isBlank(b4)) {
                sb.append("; buvid4=").append(b4);
            }
            return sb.toString();
        } catch (Exception e) {
            log.debug("获取 buvid (spi) 失败: {}", e.getMessage());
            return null;
        }
    }

    private String fetchCookieFromHome() {
        Request request = new Request.Builder()
                .url(HOME_URL)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            List<String> setCookies = response.headers("Set-Cookie");
            for (String sc : setCookies) {
                if (sc.startsWith("buvid3=")) {
                    return sc.split(";", 2)[0];
                }
            }
        } catch (Exception e) {
            log.debug("获取 buvid (home) 失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 按学生在对话中提出的主题检索学习视频，返回多条候选资源卡片。
     * 用于 AI 对话中的"帮我找资料/推荐视频"场景。
     */
    public List<Map<String, Object>> searchLearningVideos(String query, int limit) {
        if (!enabled || isBlank(query)) {
            return List.of();
        }
        try {
            String core = coreTerm(query);
            List<String> keywords = new ArrayList<>();
            // 全部偏向"学习/教学"语义，避免被娱乐、游戏内容带偏
            keywords.add(query + " 教程");
            keywords.add(query + " 教学");
            keywords.add(query + " 公开课");
            keywords.add(query + " 讲解");
            if (!core.equals(query)) {
                keywords.add(core + " 教程");
            }
            if (!preferredAuthors.isEmpty()) {
                keywords.add(preferredAuthors.get(0) + " " + query);
            }

            Map<String, Map<String, Object>> unique = new LinkedHashMap<>();
            for (String keyword : keywords) {
                try {
                    for (Map<String, Object> candidate : searchByKeyword(keyword, query, null, 1)) {
                        // 只保留"学习类"且与主题相关的视频，过滤娱乐/游戏
                        if (isLearningVideo(
                                valueOrEmpty(candidate.get("title")),
                                valueOrEmpty(candidate.get("author")),
                                query)) {
                            unique.putIfAbsent(String.valueOf(candidate.get("bvid")), candidate);
                        }
                    }
                } catch (Exception e) {
                    log.debug("对话视频搜索失败 keyword={}: {}", keyword, e.getMessage());
                }
                if (unique.size() >= 30) {
                    break;
                }
            }

            List<Map<String, Object>> cards = unique.values().stream()
                    .sorted(Comparator.comparingInt(item -> -toInt(item.get("score"))))
                    .limit(Math.max(1, limit))
                    .map(this::toResourceCard)
                    .collect(Collectors.toList());
            // B站接口在部分网络/IP 下会被风控降级（返回空），此时回退到多平台搜索直达卡片，
            // 保证学生索要视频时始终有可点击的权威学习入口（搜索页在用户本机浏览器可正常打开）。
            if (cards.isEmpty()) {
                return buildSearchLinkCards(query);
            }
            return cards;
        } catch (Exception e) {
            log.warn("对话视频检索失败: {}", e.getMessage());
            return buildSearchLinkCards(query);
        }
    }

    /**
     * 多平台学习资源搜索直达卡片：当真实视频检索不可用时的稳定兜底。
     * 指向中国大学MOOC、B站、学堂在线的搜索结果页，用户点击即在本机浏览器查看。
     */
    private List<Map<String, Object>> buildSearchLinkCards(String query) {
        if (isBlank(query)) {
            return List.of();
        }
        String q = urlEncode(query.trim());
        List<Map<String, Object>> cards = new ArrayList<>();
        cards.add(searchLinkCard("中国大学MOOC", "国家级精品课程与高校公开课",
                "https://www.icourse163.org/search.htm?search=" + q, true));
        cards.add(searchLinkCard("哔哩哔哩", "海量教学视频与名校公开课",
                "https://search.bilibili.com/all?keyword=" + q + "%20教程", false));
        cards.add(searchLinkCard("学堂在线", "清华出品的慕课平台",
                "https://www.xuetangx.com/search?query=" + q, true));
        return cards;
    }

    private Map<String, Object> searchLinkCard(String platform, String desc, String url, boolean preferred) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("type", "search-link");
        card.put("source", platform);
        card.put("title", "在「" + platform + "」搜索相关学习资源");
        card.put("author", platform);
        card.put("description", desc);
        card.put("preferredAuthor", preferred);
        card.put("url", url);
        return card;
    }

    private String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private static final String[] LEARNING_SIGNALS = {
            "教程", "课程", "公开课", "讲解", "入门", "原理", "详解", "教学", "学习",
            "笔记", "复习", "精讲", "导论", "概论", "基础", "实验课", "课堂", "课时",
            "知识点", "考研", "期末", "速成", "从零", "零基础", "tutorial", "lesson", "course"
    };

    private static final String[] ENTERTAINMENT_BLOCK = {
            "游戏", "实况", "直播回放", "直播录像", "鬼畜", "搞笑", "整活", "王者荣耀",
            "英雄联盟", "无畏契约", "瓦罗兰特", "绝地求生", "和平精英", "原神", "明日方舟",
            "csgo", "cs2", "lol", "吃鸡", "集锦", "高光", "操作", "赛事", "电竞", "主播",
            "cos", "动漫", "番剧", "舞蹈", "翻跳", "vlog", "开箱", "鬼灭", "mv", "音乐现场",
            "解说", "实机", "通关", "速通", "联机"
    };

    /**
     * 判断候选视频是否为"学习类"且与主题相关：
     * 必须与主题相关（标题含主题/核心词，或字面重叠足够），
     * 且带有教学信号或来自权威课程作者，并排除明显的娱乐/游戏内容。
     */
    private boolean isLearningVideo(String title, String author, String query) {
        if (isBlank(title)) {
            return false;
        }
        String lowerTitle = title.toLowerCase();
        // 1. 娱乐/游戏负向过滤
        for (String block : ENTERTAINMENT_BLOCK) {
            if (lowerTitle.contains(block.toLowerCase())) {
                return false;
            }
        }
        // 2. 主题相关性：标题需命中主题/核心词，或命中某个主题词；
        //    只有当主题足够长(>=4字)时才允许二元组模糊兜底，避免“重新/认识”等短常用词误匹配。
        String core = coreTerm(query);
        boolean relevant = contains(title, query)
                || (!core.isBlank() && contains(title, core));
        if (!relevant && !isBlank(query)) {
            for (String token : query.split("\\s+")) {
                if (token.length() >= 2 && contains(title, token)) {
                    relevant = true;
                    break;
                }
            }
        }
        if (!relevant) {
            String compact = query == null ? "" : query.replaceAll("\\s+", "");
            if (compact.length() >= 4 && bigramOverlap(query, title) >= 0.6) {
                relevant = true;
            }
        }
        if (!relevant) {
            return false;
        }
        // 3. 学习信号：标题含教学类词，或来自权威课程平台/高校
        boolean learningSignal = isPreferredAuthor(author, "");
        if (!learningSignal) {
            for (String signal : LEARNING_SIGNALS) {
                if (lowerTitle.contains(signal.toLowerCase())) {
                    learningSignal = true;
                    break;
                }
            }
        }
        return learningSignal;
    }

    private Map<String, Object> toResourceCard(Map<String, Object> candidate) {
        String bvid = String.valueOf(candidate.get("bvid"));
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("type", "video");
        card.put("source", "bilibili");
        card.put("bvid", bvid);
        card.put("title", candidate.get("title"));
        card.put("author", candidate.get("author"));
        card.put("description", candidate.get("description"));
        card.put("duration", candidate.get("duration"));
        card.put("playCount", candidate.get("playCount"));
        card.put("preferredAuthor", candidate.get("preferredAuthor"));
        card.put("url", "https://www.bilibili.com/video/" + bvid);
        card.put("embedUrl", "https://player.bilibili.com/player.html?bvid=" + bvid
                + "&autoplay=0&danmaku=0&high_quality=1&quality=80&qn=80");
        return card;
    }

    public Map<String, Object> findBestVideo(String knowledgePointId,
                                             String knowledgePointName,
                                             String moduleName,
                                             String strategyLabel) {
        if (!enabled || isBlank(knowledgePointName)) {
            return null;
        }

        try {
            List<Map<String, Object>> candidates = searchCandidates(knowledgePointName, moduleName);
            if (candidates.isEmpty()) {
                return null;
            }

            Map<String, Object> selected = chooseWithAi(
                    candidates, knowledgePointId, knowledgePointName, moduleName, strategyLabel);
            if (selected == null) {
                selected = candidates.get(0);
                selected.put("reason", "系统按标题与当前知识点的匹配度自动推荐。");
                selected.put("confidence", 0.6);
                selected.put("selectionMode", "heuristic");
            }

            selected.put("source", "bilibili");
            selected.put("preferredAuthor", isPreferredAuthor(
                    valueOrEmpty(selected.get("author")), valueOrEmpty(selected.get("authorMid"))));
            selected.put("url", "https://www.bilibili.com/video/" + selected.get("bvid"));
            selected.put("embedUrl", "https://player.bilibili.com/player.html?bvid="
                    + selected.get("bvid") + "&autoplay=0&danmaku=0&high_quality=1&quality=80&qn=80");
            selected.put("requestedQuality", "1080P");
            return selected;
        } catch (Exception e) {
            log.warn("课程视频匹配失败: {}", e.getMessage());
            return null;
        }
    }

    private List<Map<String, Object>> searchCandidates(String knowledgePointName, String moduleName) {
        Map<String, Map<String, Object>> unique = new LinkedHashMap<>();
        for (String keyword : buildKeywords(knowledgePointName, moduleName)) {
            try {
                for (int page = 1; page <= 3; page++) {
                    for (Map<String, Object> candidate : searchByKeyword(keyword, knowledgePointName, moduleName, page)) {
                        unique.putIfAbsent(String.valueOf(candidate.get("bvid")), candidate);
                    }
                    if (unique.size() >= 40) {
                        break;
                    }
                }
            } catch (Exception e) {
                log.debug("B站搜索失败 keyword={}: {}", keyword, e.getMessage());
            }
            if (unique.size() >= 40) {
                break;
            }
        }

        return unique.values().stream()
                .sorted(Comparator.comparingInt(item -> -toInt(item.get("score"))))
                .limit(12)
                .collect(Collectors.toList());
    }

    /**
     * 播放量热度加成（对数尺度，最高约 +50）。
     * 相关性门槛（score &lt; 18 直接淘汰）在本加成之前应用，因此提高权重只会
     * 在“已相关”的候选中明显优先高播放量视频，不会引入无关内容。
     * 量级参考：1千≈24、1万≈32、10万≈40、100万≈48。
     */
    private int popularityBonus(long playCount) {
        if (playCount <= 0) {
            return 0;
        }
        double bonus = Math.log10(playCount + 1) * 8.0;
        return (int) Math.min(50, Math.round(bonus));
    }

    private Set<String> buildKeywords(String knowledgePointName, String moduleName) {
        Set<String> keywords = new LinkedHashSet<>();
        String core = coreTerm(knowledgePointName);
        // 优先用权威课程平台/高校作为搜索词，提升资源权威性
        if (!preferredAuthors.isEmpty()) {
            keywords.add(preferredAuthors.get(0) + " " + knowledgePointName);
            keywords.add(preferredAuthors.get(0) + " " + core);
        }
        keywords.add(knowledgePointName);
        keywords.add(knowledgePointName + " 教程");
        keywords.add(knowledgePointName + " 课程讲解");
        keywords.add(knowledgePointName + " 公开课");
        // 核心词变体（去掉"基础/初步/概述"等通用词后命中更广）
        if (!core.equals(knowledgePointName) && !core.isBlank()) {
            keywords.add(core);
            keywords.add(core + " 教程");
            keywords.add(core + " 入门");
        }
        if (!isBlank(moduleName)) {
            keywords.add(moduleName + " " + core);
            keywords.add(moduleName + " 课程");
        }
        keywords.add(knowledgePointName + " 实战");
        return keywords;
    }

    /**
     * 提取知识点核心词：去掉常见的通用后缀/前缀，提升搜索与匹配的召回率。
     * 例：机器学习基础 -> 机器学习；数字电路初步 -> 数字电路；XX概述 -> XX
     */
    private String coreTerm(String name) {
        if (isBlank(name)) {
            return "";
        }
        String core = name.trim();
        String[] suffixes = {"基础知识", "基础", "初步", "概述", "入门", "进阶", "简介",
                "与应用", "及应用", "应用", "详解", "原理", "导论", "概论", "基本概念", "概念"};
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String suffix : suffixes) {
                if (core.length() > suffix.length() + 1 && core.endsWith(suffix)) {
                    core = core.substring(0, core.length() - suffix.length()).trim();
                    changed = true;
                }
            }
        }
        return core.isEmpty() ? name.trim() : core;
    }

    private List<Map<String, Object>> searchByKeyword(String keyword,
                                                      String knowledgePointName,
                                                      String moduleName,
                                                      int page) throws IOException {
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(SEARCH_API)).newBuilder()
                .addQueryParameter("search_type", "video")
                .addQueryParameter("page", String.valueOf(page))
                .addQueryParameter("order", "totalrank")
                .addQueryParameter("keyword", keyword)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36")
                .addHeader("Referer", "https://search.bilibili.com")
                .addHeader("Accept", "application/json, text/plain, */*")
                .addHeader("Accept-Language", "zh-CN,zh;q=0.9")
                .addHeader("Origin", "https://www.bilibili.com")
                .addHeader("Cookie", ensureCookie())
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Bilibili search HTTP " + response.code());
            }
            ResponseBody body = response.body();
            if (body == null) {
                return List.of();
            }

            JsonNode root = objectMapper.readTree(body.string());
            int code = root.path("code").asInt(0);
            if (code != 0) {
                // -412 风控 / -799 频繁 等：清掉缓存 Cookie，下次重新获取
                log.warn("Bilibili 搜索被拦截 code={} msg={} keyword={}",
                        code, root.path("message").asText(""), keyword);
                if (code == -412 || code == -352) {
                    cookieHeader = null;
                }
                return List.of();
            }
            JsonNode result = root.path("data").path("result");
            if (!result.isArray()) {
                return List.of();
            }

            List<Map<String, Object>> candidates = new ArrayList<>();
            for (JsonNode item : result) {
                String bvid = item.path("bvid").asText("");
                String author = clean(item.path("author").asText(""));
                String mid = item.path("mid").asText("");
                if (isBlank(bvid)) {
                    continue;
                }

                String title = clean(item.path("title").asText(""));
                String description = clean(item.path("description").asText(""));
                boolean preferredAuthor = isPreferredAuthor(author, mid);
                int score = score(title, description, knowledgePointName, moduleName, preferredAuthor);
                if (score <= 0) {
                    continue;
                }

                long playCount = item.path("play").asLong(0);
                score += popularityBonus(playCount);

                Map<String, Object> candidate = new LinkedHashMap<>();
                candidate.put("bvid", bvid);
                candidate.put("title", title);
                candidate.put("description", description);
                candidate.put("author", author);
                candidate.put("authorMid", mid);
                candidate.put("preferredAuthor", preferredAuthor);
                candidate.put("duration", clean(item.path("duration").asText("")));
                candidate.put("playCount", playCount);
                candidate.put("keyword", keyword);
                candidate.put("score", score);
                candidates.add(candidate);
            }
            return candidates;
        }
    }

    private Map<String, Object> chooseWithAi(List<Map<String, Object>> candidates,
                                             String knowledgePointId,
                                             String knowledgePointName,
                                             String moduleName,
                                             String strategyLabel) {
        try {
            String systemPrompt = "你是高校课程学习资源推荐器。"
                    + "只能从用户给出的候选视频中选择一个，不能编造 BV 号。"
                    + "选择原则：在与当前知识点都相关的前提下，优先选择 playCount（播放量）更高、更受欢迎的视频。"
                    + "返回严格 JSON：{\"selectedBvid\":\"BV...\",\"reason\":\"一句中文推荐理由\",\"confidence\":0.0}";
            String userMessage = "当前知识点ID：" + valueOrEmpty(knowledgePointId) + "\n"
                    + "当前知识点：" + knowledgePointName + "\n"
                    + "模块：" + valueOrEmpty(moduleName) + "\n"
                    + "教学策略：" + valueOrEmpty(strategyLabel) + "\n"
                    + "候选视频：" + objectMapper.writeValueAsString(candidates);

            String response = llmService.chatJsonSync(userMessage, systemPrompt, 0.2);
            Map<String, Object> selectedData = objectMapper.readValue(
                    response, new TypeReference<Map<String, Object>>() {});
            String selectedBvid = valueOrEmpty(selectedData.get("selectedBvid"));
            if (isBlank(selectedBvid)) {
                return null;
            }

            for (Map<String, Object> candidate : candidates) {
                if (selectedBvid.equals(candidate.get("bvid"))) {
                    Map<String, Object> selected = new LinkedHashMap<>(candidate);
                    selected.put("reason", firstNonBlank(selectedData.get("reason"),
                            "AI 根据当前知识点与视频标题简介的相关性推荐。"));
                    selected.put("confidence", selectedData.getOrDefault("confidence", 0.75));
                    selected.put("selectionMode", "ai");
                    return selected;
                }
            }
        } catch (Exception e) {
            log.debug("AI 视频选择失败，使用启发式排序: {}", e.getMessage());
        }
        return null;
    }

    private boolean isPreferredAuthor(String author, String mid) {
        if (isBlank(author) || preferredAuthors.isEmpty()) {
            return false;
        }
        return preferredAuthors.stream()
                .anyMatch(preferred -> author.equals(preferred) || author.contains(preferred));
    }

    private int score(String title,
                      String description,
                      String knowledgePointName,
                      String moduleName,
                      boolean preferredAuthor) {
        String core = coreTerm(knowledgePointName);
        int score = 0;

        // 标题相关性：全名 > 核心词 > 二元组模糊重叠
        if (contains(title, knowledgePointName)) {
            score += 60;
        } else if (!core.equals(knowledgePointName) && contains(title, core)) {
            score += 45;
        }
        double titleOverlap = bigramOverlap(knowledgePointName, title);
        score += (int) Math.round(titleOverlap * 40);

        // 简介相关性
        if (contains(description, knowledgePointName)
                || (!core.isBlank() && contains(description, core))) {
            score += 15;
        } else {
            score += (int) Math.round(bigramOverlap(knowledgePointName, description) * 10);
        }

        // 模块/课程命中
        if (!isBlank(moduleName) && contains(title, moduleName)) {
            score += 8;
        }
        // 教学类信号
        if (contains(title, "教程") || contains(title, "课程") || contains(title, "公开课")
                || contains(title, "讲解") || contains(title, "入门") || contains(title, "实战")) {
            score += 6;
        }

        // 相关性门槛：至少命中核心词或有足够的字面重叠，避免引入无关视频
        if (score < 18) {
            return 0;
        }

        if (preferredAuthor) score += 70;
        if (contains(title, "直播") || contains(title, "回放") || contains(title, "切片")) score -= 15;
        return score;
    }

    /**
     * 计算 name 的二元组在 text 中出现的比例（中文模糊匹配），返回 0~1。
     */
    private double bigramOverlap(String name, String text) {
        if (isBlank(name) || isBlank(text)) {
            return 0.0;
        }
        String compactName = name.replaceAll("\\s+", "");
        if (compactName.length() < 2) {
            return text.contains(compactName) ? 1.0 : 0.0;
        }
        Set<String> bigrams = new LinkedHashSet<>();
        for (int i = 0; i + 2 <= compactName.length(); i++) {
            bigrams.add(compactName.substring(i, i + 2));
        }
        if (bigrams.isEmpty()) {
            return 0.0;
        }
        int hit = 0;
        for (String bigram : bigrams) {
            if (text.contains(bigram)) {
                hit++;
            }
        }
        return (double) hit / bigrams.size();
    }

    private boolean contains(String text, String keyword) {
        return !isBlank(text) && !isBlank(keyword) && text.contains(keyword);
    }

    private String clean(String value) {
        if (value == null) return "";
        return HTML_TAG.matcher(value)
                .replaceAll("")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&#39;", "'")
                .trim();
    }

    private String valueOrEmpty(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String firstNonBlank(Object... values) {
        for (Object value : values) {
            String text = valueOrEmpty(value);
            if (!text.isBlank()) return text;
        }
        return "";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private int toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }
}
