package com.fitness.management.service;

import com.fitness.management.dto.chat.ChatStreamRequestDto;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 智能对话（SSE 流式）。
 */
public interface ChatService {

    /**
     * 异步启动流式对话：频率限制、上下文增强、调用大模型并通过 {@link SseEmitter} 推送。
     */
    void startStreamChat(Long userId, ChatStreamRequestDto dto, SseEmitter emitter);
}
