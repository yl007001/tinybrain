package com.tinybrain.mcp;

import com.tinybrain.mcp.dto.MCPRequest;
import com.tinybrain.mcp.dto.MCPResponse;
import com.tinybrain.mcp.transport.Transport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MCPClientTest {

    @Mock
    private Transport transport;

    private MCPClient mcpClient;

    @BeforeEach
    void setUp() {
        mcpClient = new MCPClient(transport);
    }

    @Test
    void testInitializeSuccess() throws Exception {
        // 准备测试数据
        MCPResponse initResponse = new MCPResponse();
        initResponse.setId(1L);
        initResponse.setResult(Map.of("protocolVersion", "2024-11-05"));

        MCPResponse listResponse = new MCPResponse();
        listResponse.setId(2L);
        listResponse.setResult(Map.of("tools", List.of(
                Map.of("name", "test_tool", "description", "Test tool")
        )));

        // 模拟传输层行为
        when(transport.sendRequest(any(MCPRequest.class)))
                .thenReturn(initResponse)
                .thenReturn(listResponse);
        when(transport.isAlive()).thenReturn(true);

        // 执行测试
        mcpClient.initialize();

        // 验证结果
        assertTrue(mcpClient.isAlive());
        assertEquals(1, mcpClient.getAvailableTools().size());
        assertEquals("test_tool", mcpClient.getAvailableTools().get(0).get("name"));
    }

    @Test
    void testInitializeFailure() throws Exception {
        // 准备测试数据
        MCPResponse errorResponse = new MCPResponse();
        errorResponse.setId(1L);
        errorResponse.setError(new MCPResponse.MCPError(-1, "Connection failed", null));

        // 模拟传输层行为
        when(transport.sendRequest(any(MCPRequest.class))).thenReturn(errorResponse);

        // 执行测试并验证异常
        assertThrows(RuntimeException.class, () -> mcpClient.initialize());
    }

    @Test
    void testCallToolSuccess() throws Exception {
        // 初始化客户端
        initializeClient();

        // 准备测试数据
        MCPResponse callResponse = new MCPResponse();
        callResponse.setId(3L);
        callResponse.setResult(Map.of("content", List.of(
                Map.of("type", "text", "text", "Tool result")
        )));

        // 模拟传输层行为
        when(transport.sendRequest(any(MCPRequest.class))).thenReturn(callResponse);

        // 执行测试
        String result = mcpClient.callTool("test_tool", Map.of("param", "value"));

        // 验证结果
        assertEquals("Tool result", result);
    }

    @Test
    void testCallToolNotInitialized() {
        // 执行测试并验证异常
        assertThrows(IllegalStateException.class, () -> 
                mcpClient.callTool("test_tool", Map.of()));
    }

    @Test
    void testGetToolSchema() throws Exception {
        // 初始化客户端
        initializeClient();

        // 执行测试
        var schema = mcpClient.getToolSchema("test_tool");

        // 验证结果
        assertNotNull(schema);
        assertEquals("object", schema.get("type").asText());
    }

    @Test
    void testClose() throws Exception {
        // 初始化客户端
        initializeClient();

        // 执行测试
        mcpClient.close();

        // 验证结果
        assertFalse(mcpClient.isAlive());
    }

    private void initializeClient() throws Exception {
        // 准备测试数据
        MCPResponse initResponse = new MCPResponse();
        initResponse.setId(1L);
        initResponse.setResult(Map.of("protocolVersion", "2024-11-05"));

        MCPResponse listResponse = new MCPResponse();
        listResponse.setId(2L);
        listResponse.setResult(Map.of("tools", List.of(
                Map.of("name", "test_tool", 
                       "description", "Test tool",
                       "inputSchema", Map.of(
                               "type", "object",
                               "properties", Map.of(
                                       "param", Map.of("type", "string", "description", "Test parameter")
                               ),
                               "required", List.of("param")
                       ))
        )));

        // 模拟传输层行为（使用 lenient 模式）
        lenient().when(transport.sendRequest(any(MCPRequest.class)))
                .thenReturn(initResponse)
                .thenReturn(listResponse);
        lenient().when(transport.isAlive()).thenReturn(true);

        // 初始化
        mcpClient.initialize();
    }
}