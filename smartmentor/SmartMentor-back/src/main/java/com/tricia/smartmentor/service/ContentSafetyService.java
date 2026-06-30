package com.tricia.smartmentor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 内容安全与防幻觉服务（满足赛题非功能性需求："具备完善的防幻觉与内容安全过滤机制，
 * 确保生成的学术内容无事实性错误、无敏感违规信息"）。
 * <p>
 * 提供三类能力，供对话与各 Agent 统一复用：
 * <ol>
 *   <li><b>内容安全</b>：对用户输入与模型输出做敏感/违规词检测与脱敏；</li>
 *   <li><b>防幻觉</b>：生成可注入 system prompt 的约束指令，要求模型不编造、
 *       不确定时显式说明、紧扣给定知识库；</li>
 *   <li><b>学术事实性</b>：附加学术内容的可靠性约束。</li>
 * </ol>
 */
@Slf4j
@Service
public class ContentSafetyService {

    /** 违规/敏感词库（精简示范，可按需扩展或接入讯飞内容安全云服务）。 */
    private static final Set<String> SENSITIVE_WORDS = new LinkedHashSet<>(Arrays.asList(
            "暴力", "色情", "赌博", "毒品", "诈骗", "邪教", "反动", "恐怖主义",
            "自杀", "自残", "枪支", "爆炸物", "制毒"
    ));

    /** 命中敏感词时对外的统一提示。 */
    public static final String BLOCKED_HINT =
            "抱歉，你的请求涉及不适合在学习场景中讨论的内容，我无法提供相关帮助。"
            + "如果你有课程学习上的问题，我很乐意帮你解答。";

    private final Pattern sensitivePattern;

    public ContentSafetyService() {
        // 预编译为一个整体正则，匹配效率更高
        String regex = String.join("|", SENSITIVE_WORDS.stream()
                .map(Pattern::quote).toArray(String[]::new));
        this.sensitivePattern = Pattern.compile(regex);
    }

    // ==================== 1. 内容安全 ====================

    /**
     * 检测文本是否命中敏感/违规词。
     * @return true 表示命中（应拦截或脱敏）
     */
    public boolean containsSensitive(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return sensitivePattern.matcher(text).find();
    }

    /**
     * 校验用户输入是否可进入大模型处理。命中违规内容时返回 false，调用方应拒绝并回复 {@link #BLOCKED_HINT}。
     */
    public boolean isInputAllowed(String userInput) {
        boolean blocked = containsSensitive(userInput);
        if (blocked) {
            log.warn("内容安全：用户输入命中敏感词，已拦截。片段长度={}", userInput == null ? 0 : userInput.length());
        }
        return !blocked;
    }

    /**
     * 对模型输出做兜底脱敏：将命中的敏感词替换为 ▇▇，避免违规内容直接呈现给学生。
     */
    public String sanitizeOutput(String modelOutput) {
        if (modelOutput == null || modelOutput.isBlank()) {
            return modelOutput;
        }
        if (!containsSensitive(modelOutput)) {
            return modelOutput;
        }
        log.warn("内容安全：模型输出命中敏感词，已脱敏");
        return sensitivePattern.matcher(modelOutput).replaceAll("▇▇");
    }

    // ==================== 2 & 3. 防幻觉 + 学术事实性约束 ====================

    /**
     * 可注入到任意 system prompt 末尾的「防幻觉 + 内容安全」约束片段，
     * 统一约束各 Agent 与对话的输出质量。
     */
    public String antiHallucinationDirective() {
        return "\n\n【内容可靠性与安全要求】\n"
                + "1. 严禁编造事实：不确定或无依据的内容必须明确说明“不确定/建议核实”，不得杜撰数据、文献、人名、公式或结论；\n"
                + "2. 紧扣已知知识：优先基于给定的知识点/课程上下文作答，超出范围时如实告知边界，不臆造；\n"
                + "3. 学术准确性：定义、定理、公式、术语须准确规范，举例须真实可验证；\n"
                + "4. 内容安全：不输出任何暴力、色情、违法、歧视、政治敏感或其他不适合学习场景的内容；\n"
                + "5. 若问题超出课程学习范畴或涉及不当内容，礼貌拒绝并引导回到课程学习。";
    }

    /**
     * 将防幻觉约束追加到给定 system prompt 之后。
     */
    public String withSafetyGuard(String systemPrompt) {
        String base = systemPrompt == null ? "" : systemPrompt;
        return base + antiHallucinationDirective();
    }

    /** 暴露词库规模，便于文档与监控展示。 */
    public int sensitiveWordCount() {
        return SENSITIVE_WORDS.size();
    }
}
