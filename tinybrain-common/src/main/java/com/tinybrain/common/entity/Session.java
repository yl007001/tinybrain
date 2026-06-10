package com.tinybrain.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话实体
 */
@Data
@TableName("ai_session")
public class Session {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话唯一标识 */
    private String sessionId;

    /** 会话标题 */
    private String title;

    /** 类型: agent/rag */
    private String type;

    /** 用户ID */
    private Long userId;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
