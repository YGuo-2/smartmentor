package com.tricia.smartmentor.controller;

import com.tricia.smartmentor.common.Result;
import com.tricia.smartmentor.config.UserPrincipal;
import com.tricia.smartmentor.dto.ProfileBuildRequest;
import com.tricia.smartmentor.dto.ProfileSettingsRequest;
import com.tricia.smartmentor.service.ConversationalProfileService;
import com.tricia.smartmentor.service.ProfileService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final ConversationalProfileService conversationalProfileService;

    public ProfileController(ProfileService profileService,
                             ConversationalProfileService conversationalProfileService) {
        this.profileService = profileService;
        this.conversationalProfileService = conversationalProfileService;
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> getProfileOverview() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Map<String, Object> overview = profileService.getProfileOverview(principal.getUserId());
        return Result.success(overview);
    }

    @PutMapping("/settings")
    public Result<Map<String, Object>> updateSettings(@RequestBody ProfileSettingsRequest request) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Map<String, Object> updated = profileService.updateSettings(principal.getUserId(), request);
        return Result.success("设置更新成功", updated);
    }

    @GetMapping("/knowledge-map")
    public Result<Map<String, Object>> getKnowledgeMap(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String depth) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Map<String, Object> knowledgeMap = profileService.getKnowledgeMap(
                principal.getUserId(), module, depth);
        return Result.success(knowledgeMap);
    }

    /**
     * 对话式画像构建：从一段对话文本抽取画像特征并写入。
     * 引导访谈结束传 overwrite=true；日常对话静默增量传 false。
     */
    @PostMapping("/build/extract")
    public Result<Map<String, Object>> buildFromConversation(@RequestBody ProfileBuildRequest request) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        boolean overwrite = Boolean.TRUE.equals(request.getOverwrite());
        Map<String, Object> result = conversationalProfileService.extractAndApply(
                principal.getUserId(), request.getConversationText(), overwrite);
        return Result.success(result);
    }

    /** 是否需要引导访谈（画像仍为空 / 出厂默认值时返回 true），供前端决定是否进入引导页。 */
    @GetMapping("/build/needed")
    public Result<Map<String, Object>> isOnboardingNeeded() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        boolean needed = profileService.isOnboardingNeeded(principal.getUserId());
        return Result.success(Map.of("needed", needed));
    }
}
