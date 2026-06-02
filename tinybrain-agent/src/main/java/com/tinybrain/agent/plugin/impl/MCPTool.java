package com.tinybrain.agent.plugin.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tinybrain.agent.plugin.AgentTool;
import com.tinybrain.mcp.MCPClient;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * MCP 工具适配器
 * <p>
 * 将 MCP 工具适配为 AgentTool 接口，使其可以被 AgentEngine 使用
 */
@Slf4j
public class MCPTool implements AgentTool {

    private final MCPClient mcpClient;
    private final String toolName;
    private final String description;
    private final ObjectNode parametersSchema;
    private final ObjectMapper objectMapper;

    public MCPTool(MCPClient mcpClient, Map<String, Object> toolDefinition) {
        this.mcpClient = mcpClient;
        this.toolName = (String) toolDefinition.get("name");
        this.description = (String) toolDefinition.get("description");
        this.objectMapper = new ObjectMapper();

        // 构建参数 Schema
        this.parametersSchema = buildParametersSchema(toolDefinition);
    }

    @Override
    public String getName() {
        return "mcp_" + toolName;
    }

    @Override
    public String getDescription() {
        return description != null ? description : "MCP 工具: " + toolName;
    }

    @Override
    public ObjectNode getParametersSchema() {
        return parametersSchema;
    }

    @Override
    public String execute(JsonNode args, ObjectMapper mapper) throws Exception {
        log.debug("执行 MCP 工具: {}", toolName);

        // 将 JsonNode 转换为 Map
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = mapper.convertValue(args, Map.class);

        // 调用 MCP 工具
        return mcpClient.callTool(toolName, arguments);
    }

    /**
     * 构建参数 Schema
     */
    @SuppressWarnings("unchecked")
    private ObjectNode buildParametersSchema(Map<String, Object> toolDefinition) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        Object inputSchema = toolDefinition.get("inputSchema");
        if (inputSchema instanceof Map) {
            Map<String, Object> inputSchemaMap = (Map<String, Object>) inputSchema;

            // 复制 properties
            Object properties = inputSchemaMap.get("properties");
            if (properties instanceof Map) {
                schema.set("properties", objectMapper.valueToTree(properties));
            } else {
                schema.set("properties", objectMapper.createObjectNode());
            }

            // 复制 required
            Object required = inputSchemaMap.get("required");
            if (required instanceof java.util.List) {
                schema.set("required", objectMapper.valueToTree(required));
            }
        } else {
            schema.set("properties", objectMapper.createObjectNode());
        }

        return schema;
    }

    /**
     * 获取原始 MCP 工具名称
     */
    public String getMcpToolName() {
        return toolName;
    }
}