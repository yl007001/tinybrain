package com.tinybrain.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tinybrain.mcp.dto.MCPRequest;
import com.tinybrain.mcp.dto.MCPResponse;
import com.tinybrain.mcp.transport.Transport;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MCP 客户端
 * <p>
 * 连接 MCP 服务器，发现和调用工具
 */
@Slf4j
public class MCPClient {

    private final Transport transport;
    private final ObjectMapper objectMapper;
    private final AtomicLong requestIdGenerator;
    private boolean initialized = false;
    private List<Map<String, Object>> availableTools = new ArrayList<>();

    public MCPClient(Transport transport) {
        this.transport = transport;
        this.objectMapper = new ObjectMapper();
        this.requestIdGenerator = new AtomicLong(1);
    }

    /**
     * 初始化 MCP 连接
     */
    public void initialize() throws Exception {
        if (initialized) {
            return;
        }

        // 初始化传输层
        transport.initialize();

        // 发送初始化请求
        MCPRequest initRequest = MCPRequest.initialize(
                requestIdGenerator.getAndIncrement(),
                "TinyBrain MCP Client"
        );

        MCPResponse response = transport.sendRequest(initRequest);
        if (response.hasError()) {
            throw new RuntimeException("MCP 初始化失败: " + response.getError().getMessage());
        }

        initialized = true;
        log.info("MCP 客户端初始化成功");

        // 获取可用工具
        refreshTools();
    }

    /**
     * 刷新可用工具列表
     */
    public void refreshTools() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("MCP 客户端未初始化");
        }

        MCPRequest listRequest = MCPRequest.listTools(requestIdGenerator.getAndIncrement());
        MCPResponse response = transport.sendRequest(listRequest);

        if (response.hasError()) {
            log.error("获取工具列表失败: {}", response.getError().getMessage());
            return;
        }

        availableTools = response.getTools();
        log.info("发现 {} 个 MCP 工具", availableTools.size());
    }

    /**
     * 获取可用工具列表
     */
    public List<Map<String, Object>> getAvailableTools() {
        return Collections.unmodifiableList(availableTools);
    }

    /**
     * 调用 MCP 工具
     *
     * @param toolName 工具名称
     * @param arguments 工具参数
     * @return 工具执行结果
     */
    public String callTool(String toolName, Map<String, Object> arguments) throws Exception {
        if (!initialized) {
            throw new IllegalStateException("MCP 客户端未初始化");
        }

        MCPRequest callRequest = MCPRequest.callTool(
                requestIdGenerator.getAndIncrement(),
                toolName,
                arguments
        );

        MCPResponse response = transport.sendRequest(callRequest);

        if (response.hasError()) {
            String errorMsg = "MCP 工具调用失败: " + response.getError().getMessage();
            log.error(errorMsg);
            return errorMsg;
        }

        String result = response.getToolResult();
        log.debug("MCP 工具 {} 调用结果: {}", toolName, result);
        return result;
    }

    /**
     * 调用 MCP 工具（JSON 参数）
     */
    public String callTool(String toolName, String argsJson) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = objectMapper.readValue(argsJson, Map.class);
        return callTool(toolName, arguments);
    }

    /**
     * 获取工具的 JSON Schema 定义
     */
    public ObjectNode getToolSchema(String toolName) {
        return availableTools.stream()
                .filter(tool -> toolName.equals(tool.get("name")))
                .findFirst()
                .map(tool -> {
                    ObjectNode schema = objectMapper.createObjectNode();
                    schema.put("type", "object");
                    Object properties = tool.get("inputSchema");
                    if (properties instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> props = (Map<String, Object>) properties;
                        schema.set("properties", objectMapper.valueToTree(props));
                    }
                    return schema;
                })
                .orElse(objectMapper.createObjectNode());
    }

    /**
     * 关闭 MCP 连接
     */
    public void close() {
        initialized = false;
        transport.close();
        log.info("MCP 客户端已关闭");
    }

    /**
     * 检查连接是否存活
     */
    public boolean isAlive() {
        return initialized && transport.isAlive();
    }
}