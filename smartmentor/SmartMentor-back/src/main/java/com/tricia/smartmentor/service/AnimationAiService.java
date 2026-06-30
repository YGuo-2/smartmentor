package com.tricia.smartmentor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 动画生成 AI 适配层。
 * <p>
 * 当前项目的 ResourceAgent 已能生成结构化分镜（scene / narration / visual / diagram）。
 * 本服务把分镜包装为“动画资产”：未配置外部文生视频服务时返回 runtime-svg，
 * 前端用 GSAP/SVG 实时播放；配置 {@code smartmentor.animation-ai.*} 后则向外部服务提交任务，
 * 并缓存 taskId/videoUrl/status 等结果。
 */
@Slf4j
@Service
public class AnimationAiService {

    private final boolean enabled;
    private final String provider;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public AnimationAiService(@Value("${animation-ai.enabled:false}") boolean enabled,
                              @Value("${animation-ai.provider:runtime-svg}") String provider,
                              @Value("${animation-ai.base-url:}") String baseUrl,
                              @Value("${animation-ai.api-key:}") String apiKey,
                              @Value("${animation-ai.model:}") String model,
                              ObjectMapper objectMapper) {
        this.enabled = enabled;
        this.provider = normalize(provider, "runtime-svg");
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null ? "" : model.trim();
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build();
        log.info("动画生成 AI 初始化：provider={}, enabled={}, configured={}",
                this.provider, enabled, isConfigured());
    }

    @PreDestroy
    public void destroy() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }

    public Map<String, Object> generateAnimation(String knowledgePointName,
                                                 String moduleName,
                                                 List<Map<String, Object>> scenes) {
        if (!isConfigured()) {
            return runtimeAsset(knowledgePointName, moduleName, scenes,
                    "未配置外部动画生成服务，使用前端实时 SVG/GSAP 动画。");
        }

        try {
            Map<String, Object> payload = buildProviderPayload(knowledgePointName, moduleName, scenes);
            Request request = new Request.Builder()
                    .url(baseUrl)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(objectMapper.writeValueAsString(payload),
                            MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                ResponseBody body = response.body();
                String bodyText = body == null ? "" : body.string();
                if (!response.isSuccessful()) {
                    throw new IOException("HTTP " + response.code() + ": " + bodyText);
                }
                Map<String, Object> asset = parseProviderResponse(bodyText);
                asset.putIfAbsent("provider", provider);
                asset.putIfAbsent("status", asset.containsKey("videoUrl") ? "ready" : "submitted");
                asset.putIfAbsent("mode", "external-video");
                asset.putIfAbsent("prompt", buildAnimationPrompt(knowledgePointName, moduleName, scenes));
                asset.putIfAbsent("scenes", scenes);
                asset.put("generatedAt", LocalDateTime.now().toString());
                return asset;
            }
        } catch (Exception e) {
            log.warn("外部动画生成失败，回退到 runtime-svg: {}", e.getMessage());
            return runtimeAsset(knowledgePointName, moduleName, scenes,
                    "外部动画生成失败，已回退为前端实时动画：" + e.getMessage());
        }
    }

    private boolean isConfigured() {
        return enabled && !apiKey.isBlank() && !baseUrl.isBlank()
                && !"runtime-svg".equalsIgnoreCase(provider);
    }

    private Map<String, Object> runtimeAsset(String knowledgePointName,
                                             String moduleName,
                                             List<Map<String, Object>> scenes,
                                             String message) {
        Map<String, Object> asset = new LinkedHashMap<>();
        asset.put("provider", "runtime-svg");
        asset.put("mode", "interactive-svg");
        asset.put("status", "fallback");
        asset.put("title", knowledgePointName + " 动画讲解");
        asset.put("moduleName", moduleName);
        asset.put("message", message);
        asset.put("prompt", buildAnimationPrompt(knowledgePointName, moduleName, scenes));
        asset.put("scenes", scenes);
        asset.put("generatedAt", LocalDateTime.now().toString());
        return asset;
    }

    private Map<String, Object> buildProviderPayload(String knowledgePointName,
                                                     String moduleName,
                                                     List<Map<String, Object>> scenes) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (!model.isBlank()) {
            payload.put("model", model);
        }
        payload.put("title", knowledgePointName + " 动画讲解");
        payload.put("prompt", buildAnimationPrompt(knowledgePointName, moduleName, scenes));
        payload.put("style", "clean educational motion graphics, high contrast labels, 16:9");
        payload.put("durationSeconds", Math.max(8, Math.min(30, scenes.size() * 5)));
        payload.put("aspectRatio", "16:9");
        payload.put("language", "zh-CN");
        payload.put("scenes", scenes);
        return payload;
    }

    private String buildAnimationPrompt(String knowledgePointName,
                                        String moduleName,
                                        List<Map<String, Object>> scenes) {
        String sceneText = scenes.stream()
                .map(scene -> {
                    String name = value(scene.get("scene"));
                    String visual = value(scene.get("visual"));
                    String narration = value(scene.get("narration"));
                    return "- " + firstNonBlank(name, "场景") + "：画面=" + visual + "；旁白=" + narration;
                })
                .collect(Collectors.joining("\n"));
        return "为高校课程《" + firstNonBlank(moduleName, "当前课程") + "》中的知识点「"
                + firstNonBlank(knowledgePointName, "当前知识点") + "」生成一段教学动画。"
                + "画面应是清晰的教育类动态图解，使用中文标签，避免卡通人物和无关装饰。"
                + "按以下分镜依次呈现：\n" + sceneText;
    }

    private Map<String, Object> parseProviderResponse(String responseText) throws IOException {
        JsonNode root = objectMapper.readTree(responseText);
        Map<String, Object> asset = new LinkedHashMap<>();
        putText(asset, "taskId", firstNodeText(root, "taskId", "task_id", "id", "data.taskId", "data.task_id"));
        putText(asset, "status", firstNodeText(root, "status", "state", "data.status", "data.state"));
        putText(asset, "videoUrl", firstNodeText(root,
                "videoUrl", "video_url", "url", "outputUrl", "output_url",
                "data.videoUrl", "data.video_url", "data.url", "data.outputUrl", "data.output_url"));
        putText(asset, "coverUrl", firstNodeText(root,
                "coverUrl", "cover_url", "posterUrl", "poster_url",
                "data.coverUrl", "data.cover_url", "data.posterUrl", "data.poster_url"));
        return asset;
    }

    private String firstNodeText(JsonNode root, String... paths) {
        for (String path : paths) {
            JsonNode node = root;
            for (String part : path.split("\\.")) {
                node = node == null ? null : node.path(part);
            }
            if (node != null && !node.isMissingNode() && !node.isNull()) {
                String text = node.asText("");
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return "";
    }

    private void putText(Map<String, Object> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    private String normalize(String value, String fallback) {
        return value == null || value.trim().isBlank() ? fallback : value.trim();
    }

    private String normalizeBaseUrl(String url) {
        if (url == null || url.trim().isBlank()) {
            return "";
        }
        return url.trim();
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "";
    }
}
