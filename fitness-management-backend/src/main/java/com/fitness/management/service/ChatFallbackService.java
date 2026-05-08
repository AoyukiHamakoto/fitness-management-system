package com.fitness.management.service;

import com.fitness.management.chat.ChatFallbackMode;
import com.fitness.management.chat.ChatUserContext;

/**
 * 大模型不可用时的固定对话降级：生成与业务上下文结合的预设中文回复（Markdown）。
 */
public interface ChatFallbackService {

    /**
     * 是否已配置可尝试调用的大模型（有 API Key 与 URL）。
     */
    boolean isLlmConfigured();

    /**
     * 组装降级全文（含提示条与关键词模板 + 用户上下文摘要）。
     */
    String composeFixedReply(ChatUserContext ctx, String userMessage, ChatFallbackMode mode);
}
