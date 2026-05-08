package com.fitness.management.chat;

/**
 * AI 对话固定话术降级的触发原因（影响提示文案）。
 */
public enum ChatFallbackMode {

    /** 未配置 API Key / URL 等，主动走预设回复 */
    NO_LLM_CONFIGURED,

    /** 已配置大模型但调用失败，降级为预设回复 */
    LLM_UNAVAILABLE
}
