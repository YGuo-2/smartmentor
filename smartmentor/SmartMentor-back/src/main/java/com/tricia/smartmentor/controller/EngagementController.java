package com.tricia.smartmentor.controller;

import com.tricia.smartmentor.common.Result;
import com.tricia.smartmentor.config.UserPrincipal;
import com.tricia.smartmentor.service.EngagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/engagement")
@RequiredArgsConstructor
public class EngagementController {

    private final EngagementService engagementService;

    /**
     * 获取今日任务列表
     */
    @GetMapping("/missions")
    public Result<?> getMissions(@RequestParam(required = false) String date) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Map<String, Object> data = engagementService.getMissions(principal.getUserId(), date);
        return Result.success(data);
    }

    /**
     * 完成任务
     */
    @PostMapping("/missions/{missionId}/complete")
    public Result<?> completeMission(@PathVariable String missionId) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Map<String, Object> data = engagementService.completeMission(principal.getUserId(), missionId);
        return Result.success(data);
    }
}
