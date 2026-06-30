package com.tricia.smartmentor.controller;

import com.tricia.smartmentor.common.Result;
import com.tricia.smartmentor.config.UserPrincipal;
import com.tricia.smartmentor.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 学习效果报告
     */
    @GetMapping("/effectiveness")
    public Result<?> getEffectivenessReport(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String period) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Map<String, Object> data = reportService.getEffectivenessReport(principal.getUserId(), module, period);
        return Result.success(data);
    }

    /**
     * 学生仪表盘
     */
    @GetMapping("/dashboard")
    public Result<?> getDashboard() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Map<String, Object> data = reportService.getDashboard(principal.getUserId());
        return Result.success(data);
    }
}
