package com.tinybrain.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinybrain.agent.core.AgentEngine;
import com.tinybrain.agent.dto.McpServerConfig;
import com.tinybrain.agent.dto.McpServerInfo;
import com.tinybrain.agent.entity.McpServer;
import com.tinybrain.agent.mapper.McpServerMapper;
import com.tinybrain.agent.plugin.impl.MCPTool;
import com.tinybrain.mcp.MCPClient;
import com.tinybrain.mcp.transport.StdioTransport;
import com.tinybrain.mcp.transport.Transport;
import lombok.Data;
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
 * 管理 MCP 服务器的配置、连接和工具发现。配置持久化到数据库，运行时状态保持在内存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpService {

    private final AgentEngine agentEngine;
    private final McpServerMapper mcpServerMapper;
    private final ObjectMapper objectMapper;

    /**
     * 活跃的 MCP 客户端（serverId -> client）
     */
    private final Map<String, MCPClient> mcpClients = new ConcurrentHashMap<>();

    /**
     * 运行时状态（serverId -> state）
     */
    private final Map<String, RuntimeState> runtimeStates = new ConcurrentHashMap<>();

    /**
     * 运行时状态内部类
     */
    @Data
    private static class RuntimeState {
        private String status = "disconnected";
        private int toolCount;
        private List<Map<String, Object>> tools = new ArrayList<>();
        private LocalDateTime lastConnectedAt;
        private String errorMessage;
    }

    // ========== 公开 API ==========

    /**
     * 获取所有启用的 MCP 服务器
     */
    public List<McpServerInfo> listServers() {
        LambdaQueryWrapper<McpServer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(McpServer::getEnabled, 1);
        List<McpServer> servers = mcpServerMapper.selectList(wrapper);
        List<McpServerInfo> result = new ArrayList<>();
        for (McpServer server : servers) {
            result.add(toInfo(server));
        }
        return result;
    }

    /**
     * 获取单个 MCP 服务器
     */
    public McpServerInfo getServer(String serverId) {
        McpServer server = getByServerId(serverId);
        return toInfo(server);
    }

    /**
     * 添加 MCP 服务器
     */
    public McpServerInfo addServer(McpServerConfig config, Long userId) {
        String serverId = UUID.randomUUID().toString();

        McpServer entity = new McpServer();
        entity.setServerId(serverId);
        entity.setName(config.getName());
        entity.setDescription(config.getDescription());
        entity.setTransport(config.getTransportType());
        entity.setCommand(config.getCommand());
        entity.setArgs(toJson(config.getArgs()));
        entity.setEnvVars(toJson(config.getEnv()));
        entity.setUrl(config.getUrl());
        entity.setEnabled(1);
        entity.setUserId(userId);

        mcpServerMapper.insert(entity);

        // 初始化运行时状态
        RuntimeState state = new RuntimeState();
        runtimeStates.put(serverId, state);

        // 如果配置了自动连接，则尝试连接
        if (config.isAutoConnect()) {
            try {
                connectServer(serverId, config);
            } catch (Exception e) {
                log.warn("自动连接 MCP 服务器失败: {}", e.getMessage());
                state.setStatus("error");
                state.setErrorMessage(e.getMessage());
            }
        }

        log.info("MCP 服务器添加成功: {} - {}", entity.getName(), serverId);
        return toInfo(entity);
    }

    /**
     * 更新 MCP 服务器
     */
    public McpServerInfo updateServer(String serverId, McpServerConfig config) {
        McpServer entity = getByServerId(serverId);

        // 断开旧连接
        disconnectServer(serverId);

        // 更新配置
        entity.setName(config.getName());
        entity.setDescription(config.getDescription());
        entity.setTransport(config.getTransportType());
        entity.setCommand(config.getCommand());
        entity.setArgs(toJson(config.getArgs()));
        entity.setEnvVars(toJson(config.getEnv()));
        entity.setUrl(config.getUrl());

        mcpServerMapper.updateById(entity);

        // 如果配置了自动连接，则尝试连接
        if (config.isAutoConnect()) {
            try {
                connectServer(serverId, config);
            } catch (Exception e) {
                log.warn("连接 MCP 服务器失败: {}", e.getMessage());
                RuntimeState state = runtimeStates.computeIfAbsent(serverId, k -> new RuntimeState());
                state.setStatus("error");
                state.setErrorMessage(e.getMessage());
            }
        }

        log.info("MCP 服务器更新成功: {} - {}", entity.getName(), serverId);
        return toInfo(entity);
    }

    /**
     * 删除 MCP 服务器
     */
    public void deleteServer(String serverId) {
        McpServer entity = getByServerId(serverId);

        // 断开连接并清理运行时状态
        disconnectServer(serverId);
        runtimeStates.remove(serverId);

        // 逻辑删除（@TableLogic 自动处理）
        mcpServerMapper.deleteById(entity.getId());

        log.info("MCP 服务器删除成功: {} - {}", entity.getName(), serverId);
    }

    /**
     * 测试 MCP 服务器连接
     */
    public Map<String, Object> testConnection(String serverId) {
        McpServer entity = getByServerId(serverId);
        McpServerConfig config = toConfig(entity);

        Map<String, Object> result = new HashMap<>();
        result.put("serverId", serverId);
        result.put("serverName", entity.getName());

        try {
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
    public List<Map<String, Object>> getServerTools(String serverId) {
        RuntimeState state = runtimeStates.get(serverId);
        if (state == null || !"connected".equals(state.getStatus())) {
            throw new RuntimeException("MCP 服务器未连接: " + serverId);
        }
        return state.getTools();
    }

    /**
     * 调用 MCP 工具
     */
    public String callTool(String serverId, String toolName, Map<String, Object> arguments) {
        MCPClient client = mcpClients.get(serverId);
        if (client == null) {
            throw new RuntimeException("MCP 服务器未连接: " + serverId);
        }

        try {
            return client.callTool(toolName, arguments);
        } catch (Exception e) {
            throw new RuntimeException("调用 MCP 工具失败: " + e.getMessage());
        }
    }

    // ========== 内部方法 ==========

    /**
     * 根据 serverId 查询数据库
     */
    private McpServer getByServerId(String serverId) {
        LambdaQueryWrapper<McpServer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(McpServer::getServerId, serverId);
        McpServer entity = mcpServerMapper.selectOne(wrapper);
        if (entity == null) {
            throw new RuntimeException("MCP 服务器不存在: " + serverId);
        }
        return entity;
    }

    /**
     * 连接 MCP 服务器
     */
    private void connectServer(String serverId, McpServerConfig config) throws Exception {
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
        mcpClients.put(serverId, client);

        // 更新运行时状态
        RuntimeState state = runtimeStates.computeIfAbsent(serverId, k -> new RuntimeState());
        state.setStatus("connected");
        state.setToolCount(tools.size());
        state.setTools(tools);
        state.setLastConnectedAt(LocalDateTime.now());
        state.setErrorMessage(null);

        log.info("MCP 服务器连接成功: {} ({} 个工具)", serverId, tools.size());
    }

    /**
     * 断开 MCP 服务器连接
     */
    private void disconnectServer(String serverId) {
        MCPClient client = mcpClients.remove(serverId);
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                log.warn("关闭 MCP 客户端失败: {}", e.getMessage());
            }
        }

        RuntimeState state = runtimeStates.get(serverId);
        if (state != null) {
            state.setStatus("disconnected");
            state.setToolCount(0);
            state.setTools(new ArrayList<>());
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
                throw new RuntimeException("暂不支持 " + config.getTransportType() + " 类型");
            default:
                throw new RuntimeException("不支持的传输类型: " + config.getTransportType());
        }
    }

    /**
     * 实体 + 运行时状态 -> DTO
     */
    private McpServerInfo toInfo(McpServer entity) {
        McpServerInfo info = new McpServerInfo();
        info.setId(entity.getServerId());
        info.setName(entity.getName());
        info.setDescription(entity.getDescription());
        info.setTransportType(entity.getTransport());
        info.setCreatedAt(entity.getCreateTime());
        info.setUpdatedAt(entity.getUpdateTime());

        // 合并运行时状态
        RuntimeState state = runtimeStates.get(entity.getServerId());
        if (state != null) {
            info.setStatus(state.getStatus());
            info.setToolCount(state.getToolCount());
            info.setTools(state.getTools());
            info.setLastConnectedAt(state.getLastConnectedAt());
            info.setErrorMessage(state.getErrorMessage());
        } else {
            info.setStatus("disconnected");
            info.setToolCount(0);
            info.setTools(new ArrayList<>());
        }

        return info;
    }

    /**
     * 实体 -> 配置 DTO（用于重建连接）
     */
    private McpServerConfig toConfig(McpServer entity) {
        McpServerConfig config = new McpServerConfig();
        config.setName(entity.getName());
        config.setDescription(entity.getDescription());
        config.setTransportType(entity.getTransport());
        config.setCommand(entity.getCommand());
        config.setArgs(parseStringArray(entity.getArgs()));
        config.setEnv(parseStringMap(entity.getEnvVars()));
        config.setUrl(entity.getUrl());
        config.setAutoConnect(false);
        return config;
    }

    // ========== JSON 工具方法 ==========

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("JSON 序列化失败: {}", e.getMessage());
            return null;
        }
    }

    private String[] parseStringArray(String json) {
        if (json == null || json.isEmpty()) return new String[0];
        try {
            return objectMapper.readValue(json, String[].class);
        } catch (JsonProcessingException e) {
            log.warn("JSON 解析失败: {}", e.getMessage());
            return new String[0];
        }
    }

    private Map<String, String> parseStringMap(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("JSON 解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 清理资源
     */
    @PreDestroy
    public void cleanup() {
        log.info("清理 MCP 服务器连接...");
        for (String serverId : new ArrayList<>(mcpClients.keySet())) {
            disconnectServer(serverId);
        }
    }
}
