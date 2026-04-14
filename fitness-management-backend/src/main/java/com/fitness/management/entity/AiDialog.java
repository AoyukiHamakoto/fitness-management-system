package com.fitness.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI对话表
 */
@Data
@TableName("ai_dialog")
public class AiDialog {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 会话ID，同一会话多轮相同 */
    private String sessionId;

    /** 角色：user 用户 / assistant 助手 */
    private String role;

    /** 消息内容 */
    private String content;

    /** 发送时间 */
    private LocalDateTime createTime;
}
