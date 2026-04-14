package com.fitness.management.controller.chat;

import com.fitness.management.dto.chat.ChatStreamRequestDto;
import com.fitness.management.security.CurrentUserId;
import com.fitness.management.security.RequireAuth;
import com.fitness.management.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.TimeUnit;

/**
 * AI 智能对话：SSE 流式输出（JWT 鉴权）。
 */
@RestController
@RequestMapping("/api/chat")
@RequireAuth
@RequiredArgsConstructor
@Validated
public class ChatController {

    private final ChatService chatService;

    /**
     * 流式对话：事件名 token为逐字增量，done 表示结束，error 为异常说明。
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@CurrentUserId Long userId, @Valid @RequestBody ChatStreamRequestDto dto) {
        SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(5));
        chatService.startStreamChat(userId, dto, emitter);
        return emitter;
    }
}
