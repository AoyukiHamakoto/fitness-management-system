package com.fitness.management.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 大模型 HTTP 接口配置（OpenAI 兼容 Chat Completions）。
 * 默认指向讯飞 MaaS 华北；其他厂商可改 api-url / model-name。
 */
@Data
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    private String apiKey = "";

    /** 完整 chat-completions URL，例如讯飞：https://maas-api.cn-huabei-1.xf-yun.com/v2/chat/completions */
    private String apiUrl = "https://maas-api.cn-huabei-1.xf-yun.com/v2/chat/completions";

    /** 控制台「模型服务」中的 modelId，例如 Qwen3.5-2B */
    private String modelName = "Qwen3.5-2B";

    /** 单次生成最大 token（计划生成 JSON 需要足够长度；0 表示不传该字段） */
    private int maxTokens = 8192;

    /**
     * 大模型未配置或调用失败时，是否使用固定话术流式降级（SSE 协议与正常对话一致）。
     */
    private boolean fallbackEnabled = true;
}
