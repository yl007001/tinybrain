package com.tinybrain.agent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * MCP 服务器配置 DTO
 */
@Data
public class McpServerConfig {

    /**
     * 服务器名称
     */
    @NotBlank(message = "服务器名称不能为空")
    private String name;

    /**
     * 服务器描述
     */
    private String description;

    /**
     * 传输类型：stdio, sse, http
     */
    @NotBlank(message = "传输类型不能为空")
    private String transportType = "stdio";

    /**
     * 命令（stdio 类型）
     */
    private String command;

    /**
     * 命令参数（stdio 类型）
     */
    private String[] args;

    /**
     * URL（sse/http 类型）
     */
    private String url;

    /**
     * 环境变量
     */
    private Map<String, String> env;

    /**
     * 是否自动连接
     */
    private boolean autoConnect = true;

    /**
     * 连接超时（毫秒）
     */
    private long connectTimeout = 10000;
}
