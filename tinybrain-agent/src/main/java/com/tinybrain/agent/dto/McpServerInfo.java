package com.tinybrain.agent.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * MCP 服务器信息 DTO
 */
@Data
public class McpServerInfo {

    /**
     * 服务器 ID
     */
    private String id;

    /**
     * 服务器名称
     */
    private String name;

    /**
     * 服务器描述
     */
    private String description;

    /**
     * 传输类型
     */
    private String transportType;

    /**
     * 连接状态：connected, disconnected, error
     */
    private String status;

    /**
     * 工具数量
     */
    private int toolCount;

    /**
     * 工具列表
     */
    private List<Map<String, Object>> tools;

    /**
     * 最后连接时间
     */
    private LocalDateTime lastConnectedAt;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
