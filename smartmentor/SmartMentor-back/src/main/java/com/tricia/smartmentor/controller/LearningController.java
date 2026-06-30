package com.tricia.smartmentor.controller;

import com.tricia.smartmentor.common.Result;
import com.tricia.smartmentor.common.RequestBodyUtils;
import com.tricia.smartmentor.config.UserPrincipal;
import com.tricia.smartmentor.service.LearningService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/learning")
public class LearningController {

    private final LearningService learningService;

    public LearningController(LearningService learningService) {
        this.learningService = learningService;
    }

    /**
     * Generate a personalized learning path
     */
    @PostMapping("/path/generate")
    public Result<?> generatePath(@RequestBody Map<String, Object> request) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Object tracingResultRef = request.get("tracingResultId");
        if (tracingResultRef == null) {
            tracingResultRef = request.get("tracingId");
        }
        String targetKnowledgePointId = RequestBodyUtils.optionalString(request, "targetKnowledgePointId");
        String mode = RequestBodyUtils.optionalString(request, "mode");
        Integer dailyStudyMinutes = RequestBodyUtils.optionalInteger(request, "dailyStudyMinutes");

        Map<String, Object> result = learningService.generatePath(
                principal.getUserId(), tracingResultRef, targetKnowledgePointId,
                mode, dailyStudyMinutes);

        return Result.success("学习路径生成成功", result);
    }

    /**
     * Get paginated list of learning paths for current student
     */
    @GetMapping("/path")
    public Result<?> getPathList(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Map<String, Object> result = learningService.getPathList(
                principal.getUserId(), status, page, size);

        return Result.success(result);
    }

    /**
     * Get full learning path details including all nodes with status
     */
    @GetMapping("/path/{pathId}")
    public Result<?> getPathDetail(@PathVariable Long pathId) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Map<String, Object> result = learningService.getPathDetail(
                principal.getUserId(), pathId);

        return Result.success(result);
    }

    /**
     * Generate personalized lesson content for a specific node
     */
    @GetMapping("/lesson/{pathId}/{nodeId}")
    public Result<?> getLesson(@PathVariable Long pathId, @PathVariable String nodeId) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Map<String, Object> result = learningService.generateLesson(
                principal.getUserId(), pathId, nodeId);

        return Result.success(result);
    }

    /**
     * 以 SSE 流式输出该节点的「讲解正文」，边生成边推送，避免长内容生成被整体超时掐断。
     * 练习题、例题、资源卡片等结构化数据仍走上面的 JSON 接口。
     */
    @GetMapping(value = "/lesson/{pathId}/{nodeId}/explain-stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLessonExplanation(@PathVariable Long pathId, @PathVariable String nodeId) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        return learningService.streamLessonExplanation(
                principal.getUserId(), pathId, nodeId);
    }

    /**
     * Find a Bilibili video resource for the current lesson node.
     */
    @GetMapping("/lesson/{pathId}/{nodeId}/video")
    public Result<?> getLessonVideo(@PathVariable Long pathId, @PathVariable String nodeId) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Map<String, Object> result = learningService.findLessonVideo(
                principal.getUserId(), pathId, nodeId);

        return Result.success(result);
    }

    /**
     * Lazily generate detailed content for the lesson resource cards.
     */
    @GetMapping("/lesson/{pathId}/{nodeId}/resource-details")
    public Result<?> getLessonResourceDetails(@PathVariable Long pathId, @PathVariable String nodeId) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Map<String, Object> result = learningService.findLessonResourceDetails(
                principal.getUserId(), pathId, nodeId);

        return Result.success(result);
    }

    /**
     * Generate or load the animation asset for the current lesson node.
     */
    @GetMapping("/lesson/{pathId}/{nodeId}/animation")
    public Result<?> getLessonAnimation(@PathVariable Long pathId, @PathVariable String nodeId) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Map<String, Object> result = learningService.findLessonAnimation(
                principal.getUserId(), pathId, nodeId);

        return Result.success(result);
    }

    /**
     * 生成（或命中快照返回）该节点的个性化演示文稿大纲（slides JSON），
     * 供前端 reveal.js 在线演示使用。
     */
    @GetMapping("/lesson/{pathId}/{nodeId}/slides")
    public Result<?> getLessonSlides(@PathVariable Long pathId, @PathVariable String nodeId) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Map<String, Object> result = learningService.findLessonSlides(
                principal.getUserId(), pathId, nodeId);

        return Result.success(result);
    }

    /**
     * 将该节点的演示文稿导出为可下载的 .pptx 文件（Apache POI 渲染）。
     */
    @GetMapping("/lesson/{pathId}/{nodeId}/slides.pptx")
    public ResponseEntity<byte[]> downloadLessonSlides(@PathVariable Long pathId,
                                                       @PathVariable String nodeId) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        LearningService.PptxFile pptx = learningService.exportLessonSlidesPptx(
                principal.getUserId(), pathId, nodeId);

        String encodedName = URLEncoder.encode(pptx.getFilename(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"presentation.pptx\"; filename*=UTF-8''" + encodedName)
                .body(pptx.getContent());
    }

    /**
     * Submit an exercise answer for evaluation
     */
    @PostMapping("/exercise/submit")
    public Result<?> submitExercise(@RequestBody Map<String, Object> request) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Long pathId = RequestBodyUtils.requiredLong(request, "pathId");
        String nodeId = RequestBodyUtils.optionalString(request, "nodeId");
        String exerciseId = RequestBodyUtils.optionalString(request, "exerciseId");
        String answer = RequestBodyUtils.optionalString(request, "answer");
        String solvingSteps = RequestBodyUtils.optionalString(request, "solvingSteps");
        Integer timeSpentSeconds = RequestBodyUtils.optionalInteger(request, "timeSpentSeconds");

        Map<String, Object> result = learningService.submitExercise(
                principal.getUserId(), pathId, nodeId, exerciseId, answer,
                solvingSteps, timeSpentSeconds);

        return Result.success(result);
    }

    /**
     * Submit checkpoint test for a learning node
     */
    @PostMapping("/checkpoint/submit")
    public Result<?> submitCheckpoint(@RequestBody Map<String, Object> request) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Long pathId = RequestBodyUtils.requiredLong(request, "pathId");
        String nodeId = RequestBodyUtils.optionalString(request, "nodeId");
        List<Map<String, Object>> answers = RequestBodyUtils.requiredObjectList(request, "answers");
        Integer totalTimeSeconds = RequestBodyUtils.optionalInteger(request, "totalTimeSeconds");

        Map<String, Object> result = learningService.submitCheckpoint(
                principal.getUserId(), pathId, nodeId, answers, totalTimeSeconds);

        return Result.success(result);
    }

}
