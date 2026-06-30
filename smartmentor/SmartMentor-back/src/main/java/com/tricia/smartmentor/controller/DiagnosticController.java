package com.tricia.smartmentor.controller;

import com.tricia.smartmentor.common.Result;
import com.tricia.smartmentor.common.RequestBodyUtils;
import com.tricia.smartmentor.config.UserPrincipal;
import com.tricia.smartmentor.service.DiagnosticService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/diagnostic")
public class DiagnosticController {

    @Autowired
    private DiagnosticService diagnosticService;

    /**
     * POST /api/diagnostic/start - Start a diagnostic session
     */
    @PostMapping("/start")
    public Result<?> startDiagnostic(@RequestBody Map<String, Object> request) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String module = RequestBodyUtils.optionalString(request, "module");
        Double difficultyValue = RequestBodyUtils.optionalDouble(request, "difficulty");
        String difficulty = difficultyValue == null ? null : difficultyValue.toString();

        if (module == null) {
            return Result.error(400, "模块参数不能为空");
        }

        Map<String, Object> data = diagnosticService.startDiagnostic(principal.getUserId(), module, difficulty, request);
        return Result.success(data);
    }

    /**
     * POST /api/diagnostic/submit - Submit an answer
     */
    @PostMapping("/submit")
    public Result<?> submitAnswer(@RequestBody Map<String, Object> request) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String diagnosticId = RequestBodyUtils.optionalString(request, "diagnosticId");
        String answer = RequestBodyUtils.optionalString(request, "answer");
        Integer timeSpent = RequestBodyUtils.optionalInteger(request, "timeSpent");

        if (diagnosticId == null) {
            return Result.error(400, "diagnosticId不能为空");
        }

        Long questionId = RequestBodyUtils.requiredLong(request, "questionId");
        Map<String, Object> data = diagnosticService.submitAnswer(principal.getUserId(), diagnosticId, questionId, answer, timeSpent);
        return Result.success(data);
    }

    /**
     * POST /api/diagnostic/finish - End a diagnostic session
     */
    @PostMapping("/finish")
    public Result<?> finishDiagnostic(@RequestBody Map<String, Object> request) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String diagnosticId = RequestBodyUtils.optionalString(request, "diagnosticId");
        if (diagnosticId == null) {
            return Result.error(400, "diagnosticId不能为空");
        }

        Map<String, Object> data = diagnosticService.finishDiagnostic(principal.getUserId(), diagnosticId);
        return Result.success(data);
    }

    /**
     * GET /api/diagnostic/history - Paginated diagnostic history
     */
    @GetMapping("/history")
    public Result<?> getHistory(
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Map<String, Object> data = diagnosticService.getHistory(principal.getUserId(), module, page, pageSize);
        return Result.success(data);
    }

    /**
     * GET /api/diagnostic/result/{diagnosticId} - Full diagnostic result
     */
    @GetMapping("/result/{diagnosticId}")
    public Result<?> getDiagnosticResult(@PathVariable String diagnosticId) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Map<String, Object> data = diagnosticService.getDiagnosticResult(principal.getUserId(), diagnosticId);
        return Result.success(data);
    }
}
