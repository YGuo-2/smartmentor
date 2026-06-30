package com.tricia.smartmentor.controller;

import com.tricia.smartmentor.common.Result;
import com.tricia.smartmentor.config.UserPrincipal;
import com.tricia.smartmentor.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * SSE streaming endpoint for AI tutoring chat
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestParam String message,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) Long pathId,
            @RequestParam(required = false) String nodeId,
            @RequestParam(required = false) String mode) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        return chatService.streamResponse(
                principal.getUserId(), message, sessionId, pathId, nodeId, mode);
    }

    /**
     * Get chat history: session list or conversation messages
     */
    @GetMapping("/history")
    public Result<?> getHistory(
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) Long pathId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Map<String, Object> result = chatService.getHistory(
                principal.getUserId(), sessionId, pathId, page, size);

        return Result.success(result);
    }
}
