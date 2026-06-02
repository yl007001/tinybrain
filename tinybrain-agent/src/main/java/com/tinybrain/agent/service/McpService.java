package com.tinybrain.agent.service;

import com.tinybrain.agent.core.AgentEngine;
import com.tinybrain.agent.dto.McpServerConfig;
import com.tinybrain.agent.dto.McpServerInfo;
import com.tinybrain.agent.plugin.impl.MCPTool;
import com.tinybrain.mcp.MCPClient;
import com.tinybrain.mcp.transport.StdioTransport;
import com.tinybrain.mcp.transport.Transport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP 服务器管理服务
 * <p>
 * 管理 MCP 服务器的配置、连接和工具发现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpService {

    private final AgentEngine agentEngine;

    /**
     * 服务器配置存储（id -> config）
     */
    private final Map<String, McpServerConfig> serverConfigs = new ConcurrentHashMap<>();

    /**
     * 服务器信息存储（id -> info）
     */
    private final Map<String, McpServerInfo> serverInfos = new ConcurrentHashMap<>();

    /**
     * MCP 客户端存储（id -> client）
     */
    private final Map<String, MCPClient> mcpClients = new ConcurrentHashMap<>();

    /**
     * 获取所有 MCP 服务器
     */
    public List<McpServerInfo> listServers() {
        return new ArrayList<>(serverInfos.values());
    }

    /**
     * 添加 MCP 服务器
     */
    public McpServerInfo addServer(McpServerConfig config) {
        String id = UUID.randomUUID().toString().substring(0, 8);

        // 保存配置
        serverConfigs.put(id, config);

        // 创建服务器信息
        McpServerInfo info = new McpServerInfo();
        info.setId(id);
        info.setName(config.getName());
        info.setDescription(config.getDescription());
        info.setTransportType(config.getTransportType());
        info.setStatus("disconnected");
        info.setToolCount(0);
        info.setTools(new ArrayList<>());
        info.setCreatedAt(LocalDateTime.now());
        info.setUpdatedAt(LocalDateTime.now());

        // 如果配置了自动连接，则尝试连接
        if (config.isAutoConnect()) {
            try {
                connectServer(id, config, info);
            } catch (Exception e) {
                log.warn("自动连接 MCP 服务器失败: {}", e.getMessage());
                info.setStatus("error");
                info.setErrorMessage(e.getMessage());
            }
        }

        serverInfos.put(id, info);
        return info;
    }

    /**
     * 更新 MCP 服务器
     */
    public McpServerInfo updateServer(String id, McpServerConfig config) {
        McpServerInfo info = serverInfos.get(id);
        if (info == null) {
            throw new RuntimeException("MCP 服务器不存在: " + id);
        }

        // 断开旧连接
        disconnectServer(id);

        // 更新配置
        serverConfigs.put(id, config);
        info.setName(config.getName());
        info.setDescription(config.getDescription());
        info.setTransportType(config.getTransportType());
        info.setUpdatedAt(LocalDateTime.now());

        // 如果配置了自动连接，则尝试连接
        if (config.isAutoConnect()) {
            try {
                connectServer(id, config, info);
            } catch (Exception e) {
                log.warn("连接 MCP 服务器失败: {}", e.getMessage());
                info.setStatus("error");
                info.setErrorMessage(e.getMessage());
            }
        }

        return info;
    }

    /**
     * 删除 MCP 服务器
     */
    public void deleteServer(String id) {
        disconnectServer(id);
        serverConfigs.remove(id);
        serverInfos.remove(id);
    }

    /**
     * 测试 MCP 服务器连接
     */
    public Map<String, Object> testConnection(String id) {
        McpServerInfo info = serverInfos.get(id);
        if (info == null) {
            throw new RuntimeException("MCP 服务器不存在: " + id);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("serverId", id);
        result.put("serverName", info.getName());

        try {
            McpServerConfig config = serverConfigs.get(id);
            Transport transport = createTransport(config);
            MCPClient client = new MCPClient(transport);
            client.initialize();

            List<Map<String, Object>> tools = client.getAvailableTools();
            result.put("success", true);
            result.put("toolCount", tools.size());
            result.put("tools", tools);

            client.close();
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 获取 MCP 服务器工具列表
     */
    public List<Map<String, Object>> getServerTools(String id) {
        McpServerInfo info = serverInfos.get(id);
        if (info == null) {
            throw new RuntimeException("MCP 服务器不存在: " + id);
        }

        if (!"connected".equals(info.getStatus())) {
            throw new RuntimeException("MCP 服务器未连接: " + info.getName());
        }

        return info.getTools();
    }

    /**
     * 调用 MCP 工具
     */
    public String callTool(String id, String toolName, Map<String, Object> arguments) {
        MCPClient client = mcpClients.get(id);
        if (client == null) {
            throw new RuntimeException("MCP 服务器未连接: " + id);
        }

        try {
            return client.callTool(toolName, arguments);
        } catch (Exception e) {
            throw new RuntimeException("调用 MCP 工具失败: " + e.getMessage());
        }
    }

    /**
     * 连接 MCP 服务器
     */
    private void connectServer(String id, McpServerConfig config, McpServerInfo info) throws Exception {
        Transport transport = createTransport(config);
        MCPClient client = new MCPClient(transport);
        client.initialize();

        // 获取工具列表
        List<Map<String, Object>> tools = client.getAvailableTools();

        // 注册 MCP 工具到 AgentEngine
        for (Map<String, Object> toolDef : tools) {
            MCPTool mcpTool = new MCPTool(client, toolDef);
            agentEngine.registerTool(mcpTool);
        }

        // 保存客户端
        mcpClients.put(id, client);

        // 更新服务器信息
        info.setStatus("connected");
        info.setToolCount(tools.size());
        info.setTools(tools);
        info.setLastConnectedAt(LocalDateTime.now());
        info.setErrorMessage(null);

        log.info("MCP 服务器连接成功: {} ({} 个工具)", info.getName(), tools.size());
    }

    /**
     * 断开 MCP 服务器连接
     */
    private void disconnectServer(String id) {
        MCPClient client = mcpClients.remove(id);
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                log.warn("关闭 MCP 客户端失败: {}", e.getMessage());
            }
        }

        McpServerInfo info = serverInfos.get(id);
        if (info != null) {
            info.setStatus("disconnected");
            info.setToolCount(0);
            info.setTools(new ArrayList<>());
        }
    }

    /**
     * 创建传输层
     */
    private Transport createTransport(McpServerConfig config) {
        switch (config.getTransportType()) {
            case "stdio":
                if (config.getCommand() == null || config.getCommand().isEmpty()) {
                    throw new RuntimeException("stdio 类型需要配置 command");
                }
                return new StdioTransport(config.getCommand(), config.getArgs(), config.getEnv());
            case "sse":
            case "http":
                // TODO: 实现 SSE/HTTP 传输层
                throw new RuntimeException("暂不支持 " + config.getTransportType() + " 类型");
            default:
                throw new RuntimeException("不支持的传输类型: " + config.getTransportType());
        }
    }

    /**
     * 清理资源
     */
    @PreDestroy
    public void cleanup() {
        log.info("清理 MCP 服务器连接...");
        for (String id : new ArrayList<>(mcpClients.keySet())) {
            disconnectServer(id);
        }
    }
}
