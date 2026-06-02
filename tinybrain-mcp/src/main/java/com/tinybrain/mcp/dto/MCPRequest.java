package com.tinybrain.mcp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 请求对象
 * <p>
 * JSON-RPC 2.0 格式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MCPRequest {

    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";

    @JsonProperty("id")
    private Object id;

    @JsonProperty("method")
    private String method;

    @JsonProperty("params")
    private Object params;

    /**
     * 创建初始化请求
     */
    public static MCPRequest initialize(Object id, String clientInfo) {
        MCPRequest request = new MCPRequest();
        request.setId(id);
        request.setMethod("initialize");
        request.setParams(new InitializeParams(clientInfo));
        return request;
    }

    /**
     * 创建工具列表请求
     */
    public static MCPRequest listTools(Object id) {
        MCPRequest request = new MCPRequest();
        request.setId(id);
        request.setMethod("tools/list");
        return request;
    }

    /**
     * 创建工具调用请求
     */
    public static MCPRequest callTool(Object id, String toolName, Object arguments) {
        MCPRequest request = new MCPRequest();
        request.setId(id);
        request.setMethod("tools/call");
        request.setParams(new ToolCallParams(toolName, arguments));
        return request;
    }

    /**
     * 初始化参数
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InitializeParams {
        @JsonProperty("clientInfo")
        private String clientInfo;
    }

    /**
     * 工具调用参数
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCallParams {
        @JsonProperty("name")
        private String name;

        @JsonProperty("arguments")
        private Object arguments;
    }
}