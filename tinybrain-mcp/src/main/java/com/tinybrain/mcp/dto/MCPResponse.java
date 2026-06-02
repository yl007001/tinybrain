package com.tinybrain.mcp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * MCP 响应对象
 * <p>
 * JSON-RPC 2.0 格式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MCPResponse {

    @JsonProperty("jsonrpc")
    private String jsonrpc = "2.0";

    @JsonProperty("id")
    private Object id;

    @JsonProperty("result")
    private Object result;

    @JsonProperty("error")
    private MCPError error;

    /**
     * 检查是否有错误
     */
    public boolean hasError() {
        return error != null;
    }

    /**
     * 获取工具列表
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getTools() {
        if (result instanceof Map) {
            Map<String, Object> resultMap = (Map<String, Object>) result;
            Object tools = resultMap.get("tools");
            if (tools instanceof List) {
                return (List<Map<String, Object>>) tools;
            }
        }
        return List.of();
    }

    /**
     * 获取工具调用结果
     */
    @SuppressWarnings("unchecked")
    public String getToolResult() {
        if (result instanceof Map) {
            Map<String, Object> resultMap = (Map<String, Object>) result;
            Object content = resultMap.get("content");
            if (content instanceof List) {
                List<Map<String, Object>> contentList = (List<Map<String, Object>>) content;
                if (!contentList.isEmpty()) {
                    Object text = contentList.get(0).get("text");
                    return text != null ? text.toString() : "";
                }
            }
        }
        return result != null ? result.toString() : "";
    }

    /**
     * MCP 错误对象
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MCPError {
        @JsonProperty("code")
        private int code;

        @JsonProperty("message")
        private String message;

        @JsonProperty("data")
        private Object data;
    }
}