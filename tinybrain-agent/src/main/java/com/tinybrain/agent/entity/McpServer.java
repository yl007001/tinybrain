package com.tinybrain.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MCP 服务器配置实体
 */
@Data
@TableName("ai_mcp_server")
public class McpServer {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 服务器唯一标识 */
    private String serverId;

    /** 服务器名称 */
    private String name;

    /** 描述 */
    private String description;

    /** 传输方式: stdio/sse */
    private String transport;

    /** 启动命令 */
    private String command;

    /** 命令参数（JSON数组） */
    private String args;

    /** 环境变量（JSON对象） */
    private String envVars;

    /** SSE 服务器 URL */
    private String url;

    /** 是否启用 */
    private Integer enabled;

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
