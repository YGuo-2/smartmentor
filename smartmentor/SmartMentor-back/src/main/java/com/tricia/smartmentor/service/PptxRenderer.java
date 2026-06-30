package com.tricia.smartmentor.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.xslf.usermodel.*;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 把演示文稿大纲（slides JSON，由 {@code PresentationAgent} 产出）渲染为 .pptx 字节流。
 * <p>
 * 纯 Apache POI（XSLF）实现，不依赖任何外部进程或 Python 环境；按 slide 的 {@code type}
 * （cover/agenda/content/code/formula/case/summary）套用不同的版式与配色。
 * 16:9 画布，版式追求清晰可读而非像素级还原。
 */
@Slf4j
@Component
public class PptxRenderer {

    // 16:9 画布尺寸（EMU 由 POI 内部按 point 换算，这里用 point）
    private static final int SLIDE_W = 960;
    private static final int SLIDE_H = 540;

    // tech-blue 主题配色
    private static final Color BRAND = new Color(0x2563EB);
    private static final Color BRAND_DARK = new Color(0x1E3A8A);
    private static final Color INK = new Color(0x1F2937);
    private static final Color MUTED = new Color(0x6B7280);
    private static final Color CODE_BG = new Color(0x0F172A);
    private static final Color CODE_FG = new Color(0xE2E8F0);
    private static final Color CARD_BG = new Color(0xF1F5F9);

    private static final String FONT = "Microsoft YaHei";

    /**
     * 渲染 slides 文档为 pptx 字节流。
     *
     * @param slidesDoc PresentationAgent 产出的文档（含 meta + slides）
     * @return pptx 文件字节
     */
    @SuppressWarnings("unchecked")
    public byte[] render(Map<String, Object> slidesDoc) {
        try (XMLSlideShow ppt = new XMLSlideShow();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ppt.setPageSize(new java.awt.Dimension(SLIDE_W, SLIDE_H));

            Map<String, Object> meta = slidesDoc.get("meta") instanceof Map
                    ? (Map<String, Object>) slidesDoc.get("meta") : Map.of();
            Object slidesObj = slidesDoc.get("slides");
            List<Object> slides = slidesObj instanceof List ? (List<Object>) slidesObj : List.of();

            for (Object s : slides) {
                if (!(s instanceof Map)) continue;
                Map<String, Object> slide = (Map<String, Object>) s;
                String type = String.valueOf(slide.getOrDefault("type", "content"));
                XSLFSlide xs = ppt.createSlide();
                switch (type) {
                    case "cover":   renderCover(xs, slide, meta); break;
                    case "agenda":  renderListSlide(xs, slide, "points"); break;
                    case "content": renderListSlide(xs, slide, "bullets"); break;
                    case "code":    renderCode(xs, slide); break;
                    case "formula": renderFormula(xs, slide); break;
                    case "case":    renderCase(xs, slide); break;
                    case "summary": renderListSlide(xs, slide, "points"); break;
                    default:        renderListSlide(xs, slide, "bullets"); break;
                }
            }

            ppt.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("演示文稿导出失败: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------ 各版式

    private void renderCover(XSLFSlide xs, Map<String, Object> slide, Map<String, Object> meta) {
        // 整页品牌色背景
        XSLFAutoShape bg = xs.createAutoShape();
        bg.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
        bg.setAnchor(new Rectangle(0, 0, SLIDE_W, SLIDE_H));
        bg.setFillColor(BRAND_DARK);
        bg.setLineColor(BRAND_DARK);

        XSLFAutoShape accent = xs.createAutoShape();
        accent.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
        accent.setAnchor(new Rectangle(80, 250, 120, 6));
        accent.setFillColor(BRAND);
        accent.setLineColor(BRAND);

        String title = str(slide.get("title"), str(meta.get("title"), "演示文稿"));
        String subtitle = str(slide.get("subtitle"), str(meta.get("subtitle"), ""));

        XSLFTextBox titleBox = xs.createTextBox();
        titleBox.setAnchor(new Rectangle(80, 170, SLIDE_W - 160, 90));
        setText(titleBox, title, 40, true, Color.WHITE, TextParagraph.TextAlign.LEFT);

        if (!subtitle.isBlank()) {
            XSLFTextBox subBox = xs.createTextBox();
            subBox.setAnchor(new Rectangle(80, 275, SLIDE_W - 160, 60));
            setText(subBox, subtitle, 20, false, new Color(0xC7D2FE), TextParagraph.TextAlign.LEFT);
        }

        String audience = str(meta.get("audience"), "");
        if (!audience.isBlank()) {
            XSLFTextBox audBox = xs.createTextBox();
            audBox.setAnchor(new Rectangle(80, 430, SLIDE_W - 160, 40));
            setText(audBox, audience, 14, false, new Color(0x93C5FD), TextParagraph.TextAlign.LEFT);
        }
    }

    private void renderListSlide(XSLFSlide xs, Map<String, Object> slide, String listKey) {
        addHeader(xs, str(slide.get("title"), "内容"));
        List<String> items = strList(slide.get(listKey));
        if (items.isEmpty()) {
            items = firstNonEmptyList(slide, "points", "bullets", "items", "content");
        }
        XSLFTextBox body = xs.createTextBox();
        body.setAnchor(new Rectangle(80, 150, SLIDE_W - 160, SLIDE_H - 200));
        boolean first = true;
        for (String item : items) {
            if (item == null || item.isBlank()) continue;
            XSLFTextParagraph p = body.addNewTextParagraph();
            first = false;
            p.setBullet(true);
            p.setIndent(0.0);
            p.setSpaceBefore(8.0);
            XSLFTextRun r = p.addNewTextRun();
            r.setText(item);
            r.setFontFamily(FONT);
            r.setFontSize(20.0);
            r.setFontColor(INK);
        }
        if (first) {
            // 无内容兜底，避免空文本框
            setText(body, "（暂无要点）", 18, false, MUTED, TextParagraph.TextAlign.LEFT);
        }
    }

    private void renderCode(XSLFSlide xs, Map<String, Object> slide) {
        addHeader(xs, str(slide.get("title"), "代码示例"));
        String explain = str(slide.get("explain"), "");
        int codeTop = 150;
        if (!explain.isBlank()) {
            XSLFTextBox exBox = xs.createTextBox();
            exBox.setAnchor(new Rectangle(80, 150, SLIDE_W - 160, 50));
            setText(exBox, explain, 16, false, MUTED, TextParagraph.TextAlign.LEFT);
            codeTop = 205;
        }
        String code = str(slide.get("code"), "");
        XSLFAutoShape codeCard = xs.createAutoShape();
        codeCard.setShapeType(org.apache.poi.sl.usermodel.ShapeType.ROUND_RECT);
        codeCard.setAnchor(new Rectangle(80, codeTop, SLIDE_W - 160, SLIDE_H - codeTop - 40));
        codeCard.setFillColor(CODE_BG);
        codeCard.setLineColor(CODE_BG);

        XSLFTextBox codeBox = xs.createTextBox();
        codeBox.setAnchor(new Rectangle(96, codeTop + 14, SLIDE_W - 192, SLIDE_H - codeTop - 68));
        boolean first = true;
        for (String line : code.split("\n")) {
            XSLFTextParagraph p = codeBox.addNewTextParagraph();
            if (first) { first = false; }
            XSLFTextRun r = p.addNewTextRun();
            r.setText(line.isEmpty() ? " " : line);
            r.setFontFamily("Consolas");
            r.setFontSize(14.0);
            r.setFontColor(CODE_FG);
        }
        if (first) {
            setText(codeBox, "// 暂无代码", 14, false, CODE_FG, TextParagraph.TextAlign.LEFT);
        }
    }

    private void renderFormula(XSLFSlide xs, Map<String, Object> slide) {
        addHeader(xs, str(slide.get("title"), "公式"));
        String latex = str(slide.get("latex"), "");
        String explain = str(slide.get("explain"), "");

        XSLFAutoShape card = xs.createAutoShape();
        card.setShapeType(org.apache.poi.sl.usermodel.ShapeType.ROUND_RECT);
        card.setAnchor(new Rectangle(80, 170, SLIDE_W - 160, 140));
        card.setFillColor(CARD_BG);
        card.setLineColor(CARD_BG);

        XSLFTextBox formulaBox = xs.createTextBox();
        formulaBox.setAnchor(new Rectangle(96, 200, SLIDE_W - 192, 80));
        // POI 不渲染 LaTeX，按等宽文本展示原式（前端 reveal.js 用 KaTeX 渲染美观版本）
        setText(formulaBox, latex.isBlank() ? "（无公式）" : latex, 24, true, BRAND_DARK,
                TextParagraph.TextAlign.CENTER, "Cambria Math");

        if (!explain.isBlank()) {
            XSLFTextBox exBox = xs.createTextBox();
            exBox.setAnchor(new Rectangle(80, 340, SLIDE_W - 160, 150));
            setText(exBox, explain, 18, false, INK, TextParagraph.TextAlign.LEFT);
        }
    }

    private void renderCase(XSLFSlide xs, Map<String, Object> slide) {
        addHeader(xs, str(slide.get("title"), "实操案例"));
        String scenario = str(slide.get("scenario"), "");
        int stepsTop = 150;
        if (!scenario.isBlank()) {
            XSLFAutoShape card = xs.createAutoShape();
            card.setShapeType(org.apache.poi.sl.usermodel.ShapeType.ROUND_RECT);
            card.setAnchor(new Rectangle(80, 150, SLIDE_W - 160, 80));
            card.setFillColor(CARD_BG);
            card.setLineColor(CARD_BG);
            XSLFTextBox scBox = xs.createTextBox();
            scBox.setAnchor(new Rectangle(96, 162, SLIDE_W - 192, 56));
            setText(scBox, scenario, 16, false, INK, TextParagraph.TextAlign.LEFT);
            stepsTop = 250;
        }
        List<String> steps = strList(slide.get("steps"));
        if (steps.isEmpty()) {
            steps = firstNonEmptyList(slide, "points", "bullets", "items", "content");
        }
        XSLFTextBox body = xs.createTextBox();
        body.setAnchor(new Rectangle(80, stepsTop, SLIDE_W - 160, SLIDE_H - stepsTop - 40));
        int i = 1;
        boolean first = true;
        for (String step : steps) {
            if (step == null || step.isBlank()) continue;
            XSLFTextParagraph p = body.addNewTextParagraph();
            first = false;
            p.setSpaceBefore(8.0);
            XSLFTextRun r = p.addNewTextRun();
            r.setText(i++ + ". " + step);
            r.setFontFamily(FONT);
            r.setFontSize(18.0);
            r.setFontColor(INK);
        }
        if (first) {
            setText(body, "（暂无步骤）", 16, false, MUTED, TextParagraph.TextAlign.LEFT);
        }
    }

    // ------------------------------------------------------------------ 通用工具

    /** 顶部标题 + 品牌色装饰条。 */
    private void addHeader(XSLFSlide xs, String title) {
        XSLFAutoShape bar = xs.createAutoShape();
        bar.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
        bar.setAnchor(new Rectangle(80, 70, 48, 6));
        bar.setFillColor(BRAND);
        bar.setLineColor(BRAND);

        XSLFTextBox titleBox = xs.createTextBox();
        titleBox.setAnchor(new Rectangle(80, 84, SLIDE_W - 160, 56));
        setText(titleBox, title, 28, true, BRAND_DARK, TextParagraph.TextAlign.LEFT);
    }

    private void setText(XSLFTextBox box, String text, int size, boolean bold,
                         Color color, TextParagraph.TextAlign align) {
        setText(box, text, size, bold, color, align, FONT);
    }

    private void setText(XSLFTextBox box, String text, int size, boolean bold,
                         Color color, TextParagraph.TextAlign align, String font) {
        XSLFTextParagraph p = box.addNewTextParagraph();
        p.setTextAlign(align);
        XSLFTextRun r = p.addNewTextRun();
        r.setText(text == null ? "" : text);
        r.setFontFamily(font);
        r.setFontSize((double) size);
        r.setBold(bold);
        r.setFontColor(color);
    }

    private String str(Object v, String fallback) {
        if (v == null) return fallback;
        String s = String.valueOf(v).trim();
        return s.isEmpty() || "null".equals(s) ? fallback : s;
    }

    @SuppressWarnings("unchecked")
    private List<String> strList(Object v) {
        List<String> items = new ArrayList<>();
        if (v instanceof List) {
            for (Object item : (List<Object>) v) {
                items.addAll(strList(item));
            }
            return items.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .collect(java.util.stream.Collectors.toList());
        }
        if (v instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) v;
            String text = str(map.get("title"), str(map.get("text"), str(map.get("content"), "")));
            return text.isBlank() ? List.of() : List.of(text);
        }
        if (v instanceof String) {
            for (String part : ((String) v).split("[\\r\\n。；;]+")) {
                String item = part.replaceFirst("^[\\-•·*\\d.、)）\\s]+", "").trim();
                if (!item.isBlank()) {
                    items.add(item);
                }
            }
            return items;
        }
        if (v != null) {
            String text = String.valueOf(v).trim();
            if (!text.isBlank() && !"null".equals(text)) {
                return List.of(text);
            }
        }
        return items;
    }

    private List<String> firstNonEmptyList(Map<String, Object> slide, String... keys) {
        for (String key : keys) {
            List<String> items = strList(slide.get(key));
            if (!items.isEmpty()) {
                return items;
            }
        }
        return List.of();
    }
}
