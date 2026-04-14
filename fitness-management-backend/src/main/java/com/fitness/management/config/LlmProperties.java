package com.fitness.management.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 大模型 HTTP 接口配置（OpenAI 兼容 Chat Completions）。
 */
@Data
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    private String apiKey = "";

    private String apiUrl = "https://api.openai.com/v1/chat/completions";

    private String modelName = "gpt-4o-mini";
}
