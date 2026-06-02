package com.tinybrain.mcp.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MCPRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testInitialize() {
        MCPRequest request = MCPRequest.initialize(1L, "Test Client");

        assertEquals("2.0", request.getJsonrpc());
        assertEquals(1L, request.getId());
        assertEquals("initialize", request.getMethod());
        assertNotNull(request.getParams());
    }

    @Test
    void testListTools() {
        MCPRequest request = MCPRequest.listTools(2L);

        assertEquals("2.0", request.getJsonrpc());
        assertEquals(2L, request.getId());
        assertEquals("tools/list", request.getMethod());
        assertNull(request.getParams());
    }

    @Test
    void testCallTool() {
        Map<String, Object> arguments = Map.of("param", "value");
        MCPRequest request = MCPRequest.callTool(3L, "test_tool", arguments);

        assertEquals("2.0", request.getJsonrpc());
        assertEquals(3L, request.getId());
        assertEquals("tools/call", request.getMethod());
        assertNotNull(request.getParams());
    }

    @Test
    void testSerialization() throws Exception {
        MCPRequest request = MCPRequest.initialize(1L, "Test Client");

        String json = objectMapper.writeValueAsString(request);
        assertNotNull(json);
        assertTrue(json.contains("\"jsonrpc\":\"2.0\""));
        assertTrue(json.contains("\"method\":\"initialize\""));
    }
}