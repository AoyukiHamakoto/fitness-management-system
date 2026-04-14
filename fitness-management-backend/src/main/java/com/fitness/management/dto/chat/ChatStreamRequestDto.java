package com.fitness.management.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 流式对话请求体。
 */
@Data
public class ChatStreamRequestDto {

    /** 会话ID，可选；不传则服务端生成新会话 */
    @Size(max = 64, message = "会话ID过长")
    private String sessionId;

    /** 用户输入 */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 8000, message = "消息过长")
    private String message;
}
