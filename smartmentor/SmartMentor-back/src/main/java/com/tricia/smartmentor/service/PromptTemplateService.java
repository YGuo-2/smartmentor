package com.tricia.smartmentor.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptTemplateService {

    private final ResourceLoader resourceLoader;
    private final Map<String, PromptTemplate> cache = new ConcurrentHashMap<>();

    public PromptTemplate load(String key, String fallbackVersion, String fallbackContent) {
        return cache.computeIfAbsent(key, currentKey -> readTemplate(currentKey, fallbackVersion, fallbackContent));
    }

    private PromptTemplate readTemplate(String key, String fallbackVersion, String fallbackContent) {
        String location = "classpath:prompts/" + key + ".md";
        try {
            Resource resource = resourceLoader.getResource(location);
            if (!resource.exists()) {
                log.warn("Prompt template not found: {}, using inline fallback", location);
                return new PromptTemplate(fallbackVersion, fallbackContent);
            }

            String raw = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            ParsedPrompt parsed = parseFrontMatter(raw);
            return new PromptTemplate(parsed.version(), parsed.content());
        } catch (Exception e) {
            log.warn("Failed to load prompt template {}, using inline fallback: {}", location, e.getMessage());
            return new PromptTemplate(fallbackVersion, fallbackContent);
        }
    }

    private ParsedPrompt parseFrontMatter(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ParsedPrompt("inline-v1", "");
        }
        String normalized = raw.replace("\r\n", "\n");
        if (!normalized.startsWith("---\n")) {
            return new ParsedPrompt("inline-v1", normalized.trim());
        }

        int end = normalized.indexOf("\n---\n", 4);
        if (end < 0) {
            return new ParsedPrompt("inline-v1", normalized.trim());
        }

        String frontMatter = normalized.substring(4, end);
        String content = normalized.substring(end + 5).trim();
        String version = "inline-v1";
        for (String line : frontMatter.split("\n")) {
            int colon = line.indexOf(':');
            if (colon <= 0) continue;
            String name = line.substring(0, colon).trim();
            String value = line.substring(colon + 1).trim();
            if ("version".equals(name) && !value.isBlank()) {
                version = value;
            }
        }
        return new ParsedPrompt(version, content);
    }

    @Getter
    @RequiredArgsConstructor
    public static class PromptTemplate {
        private final String version;
        private final String content;
    }

    private record ParsedPrompt(String version, String content) {
    }
}
