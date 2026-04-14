package com.fitness.management.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fitness.management.ai.AIResponseHandler;
import com.fitness.management.common.ResultCode;
import com.fitness.management.config.LlmProperties;
import com.fitness.management.exception.BusinessException;
import com.fitness.management.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * HTTP 调用大模型：兼容 Bearer + JSON Body的主流 Chat Completions 接口。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @Override
    public String callAI(String systemPrompt, String userPrompt) {
        assertConfigured();
        try {
            String body = buildChatBody(systemPrompt, userPrompt, false);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(llmProperties.getApiUrl()))
                    .timeout(Duration.ofMinutes(3))
                    .header("Authorization", "Bearer " + llmProperties.getApiKey().trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("大模型HTTP错误 status={} body={}", response.statusCode(), response.body());
                throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "大模型接口调用失败");
            }
            JsonNode root = objectMapper.readTree(response.body());
            if (root.has("error")) {
                String msg = root.path("error").path("message").asText("未知错误");
                throw new BusinessException("大模型返回错误: " + msg);
            }
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new BusinessException("大模型返回内容为空");
            }
            return choices.get(0).path("message").path("content").asText("").trim();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("同步调用大模型异常", e);
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "大模型调用异常: " + e.getMessage());
        }
    }

    @Override
    public void streamChat(String systemPrompt, String userPrompt, Consumer<String> sseDataLineConsumer) {
        assertConfigured();
        try {
            String body = buildChatBody(systemPrompt, userPrompt, true);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(llmProperties.getApiUrl()))
                    .timeout(Duration.ofMinutes(5))
                    .header("Authorization", "Bearer " + llmProperties.getApiKey().trim())
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String err = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                log.warn("大模型流式HTTP错误 status={} body={}", response.statusCode(), err);
                throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "大模型流式接口调用失败");
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data:")) {
                        String payload = line.substring(5).trim();
                        if ("[DONE]".equals(payload)) {
                            break;
                        }
                        sseDataLineConsumer.accept(payload);
                    }
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("流式调用大模型异常", e);
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "大模型流式调用异常: " + e.getMessage());
        }
    }

    @Override
    public void streamChatWithHandler(String systemPrompt, String userPrompt, AIResponseHandler handler) {
        assertConfigured();
        StringBuilder full = new StringBuilder();
        try {
            streamChat(systemPrompt, userPrompt, payload -> {
                if ("[DONE]".equals(payload)) {
                    return;
                }
                try {
                    JsonNode node = objectMapper.readTree(payload);
                    if (node.has("error")) {
                        String msg = node.path("error").path("message").asText("大模型返回错误");
                        throw new BusinessException(msg);
                    }
                    JsonNode choices = node.path("choices");
                    if (choices.isArray() && !choices.isEmpty()) {
                        String piece = choices.get(0).path("delta").path("content").asText("");
                        if (piece != null && !piece.isEmpty()) {
                            full.append(piece);
                            for (int i = 0; i < piece.length(); i++) {
                                handler.onToken(String.valueOf(piece.charAt(i)));
                            }
                        }
                    }
                } catch (BusinessException e) {
                    throw new RuntimeException(e);
                } catch (Exception e) {
                    log.debug("忽略无法解析的流式片段: {}", payload);
                }
            });
            handler.onComplete(full.toString());
        } catch (RuntimeException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            handler.onError(cause);
        } catch (Exception e) {
            handler.onError(e);
        }
    }

    private void assertConfigured() {
        if (!StringUtils.hasText(llmProperties.getApiKey())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "未配置大模型 API Key（llm.api-key）");
        }
        if (!StringUtils.hasText(llmProperties.getApiUrl())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "未配置大模型 API地址（llm.api-url）");
        }
    }

    private String buildChatBody(String systemPrompt, String userPrompt, boolean stream) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", llmProperties.getModelName());
        root.put("stream", stream);
        root.put("temperature", 0.35);
        ArrayNode messages = root.putArray("messages");
        ObjectNode sys = messages.addObject();
        sys.put("role", "system");
        sys.put("content", systemPrompt);
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", userPrompt);
        return objectMapper.writeValueAsString(root);
    }
}
