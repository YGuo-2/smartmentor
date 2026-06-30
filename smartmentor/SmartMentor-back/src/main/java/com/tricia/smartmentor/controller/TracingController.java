package com.tricia.smartmentor.controller;

import com.tricia.smartmentor.common.Result;
import com.tricia.smartmentor.common.RequestBodyUtils;
import com.tricia.smartmentor.config.UserPrincipal;
import com.tricia.smartmentor.service.TracingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tracing")
public class TracingController {

    @Autowired
    private TracingService tracingService;

    /**
     * POST /api/tracing/analyze - Analyze knowledge tracing
     */
    @PostMapping("/analyze")
    public Result<?> analyze(@RequestBody Map<String, Object> request) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String diagnosticId = RequestBodyUtils.optionalString(request, "diagnosticId");
        List<String> knowledgePointIds = RequestBodyUtils.optionalStringList(request, "knowledgePointIds");
        Integer maxDepth = RequestBodyUtils.optionalInteger(request, "maxDepth");
        Double masteryThreshold = RequestBodyUtils.optionalDouble(request, "masteryThreshold");

        if (diagnosticId == null && (knowledgePointIds == null || knowledgePointIds.isEmpty())) {
            return Result.error(400, "必须提供 diagnosticId 或 knowledgePointIds");
        }

        Map<String, Object> data = tracingService.analyze(principal.getUserId(), diagnosticId, knowledgePointIds, maxDepth, masteryThreshold);
        return Result.success(data);
    }

    /**
     * GET /api/tracing/result/{tracingId} - Get full tracing result
     */
    @GetMapping("/result/{tracingId}")
    public Result<?> getTracingResult(@PathVariable String tracingId) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Map<String, Object> data = tracingService.getTracingResult(principal.getUserId(), tracingId);
        return Result.success(data);
    }
}
