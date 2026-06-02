package com.tinybrain.mcp.transport;

import com.tinybrain.mcp.dto.MCPRequest;
import com.tinybrain.mcp.dto.MCPResponse;

/**
 * MCP 传输层接口
 * <p>
 * 支持多种传输方式：stdio、SSE、WebSocket
 */
public interface Transport {

    /**
     * 初始化传输连接
     */
    void initialize() throws Exception;

    /**
     * 发送请求并接收响应
     *
     * @param request MCP 请求
     * @return MCP 响应
     */
    MCPResponse sendRequest(MCPRequest request) throws Exception;

    /**
     * 关闭传输连接
     */
    void close();

    /**
     * 检查连接是否存活
     */
    boolean isAlive();
}