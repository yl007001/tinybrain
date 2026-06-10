package com.tinybrain.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对话消息实体
 */
@Data
@TableName("ai_message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话ID */
    private String sessionId;

    /** 角色: user/assistant/system */
    private String role;

    /** 消息内容 */
    private String content;

    /** 工具调用记录（JSON） */
    private String toolCalls;

    /** 用户ID */
    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
