package com.fitness.management.service;

import com.fitness.management.ai.AIResponseHandler;

import java.util.function.Consumer;

/**
 * 大模型调用封装：同步对话与流式输出（OpenAI 兼容 Chat Completions）。
 */
public interface AIService {

    /**
     * 同步调用，返回助手完整文本内容（不含 SSE 包装）。
     *
     * @param systemPrompt 系统提示词，约束输出格式与角色
     * @param userPrompt   用户侧提示词或拼装后的用户消息
     */
    String callAI(String systemPrompt, String userPrompt);

    /**
     * 流式调用：按 SSE 数据块回调原始 JSON 行（data: 之后的内容），由上层自行解析 delta。
     * 若服务端不支持流式，实现可退化为一次回调完整文本。
     */
    void streamChat(String systemPrompt, String userPrompt, Consumer<String> sseDataLineConsumer);

    /**
     * 流式调用并解析 OpenAI 兼容的 delta，按字符触发 {@link AIResponseHandler#onToken}，结束时 {@link AIResponseHandler#onComplete}。
     */
    void streamChatWithHandler(String systemPrompt, String userPrompt, AIResponseHandler handler);
}
