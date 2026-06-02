package com.tinybrain.mcp.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MCPResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testHasError() {
        MCPResponse response = new MCPResponse();
        assertFalse(response.hasError());

        response.setError(new MCPResponse.MCPError(-1, "Error", null));
        assertTrue(response.hasError());
    }

    @Test
    void testGetTools() {
        MCPResponse response = new MCPResponse();
        response.setResult(Map.of("tools", List.of(
                Map.of("name", "tool1", "description", "Tool 1"),
                Map.of("name", "tool2", "description", "Tool 2")
        )));

        List<Map<String, Object>> tools = response.getTools();
        assertEquals(2, tools.size());
        assertEquals("tool1", tools.get(0).get("name"));
        assertEquals("tool2", tools.get(1).get("name"));
    }

    @Test
    void testGetToolResult() {
        MCPResponse response = new MCPResponse();
        response.setResult(Map.of("content", List.of(
                Map.of("type", "text", "text", "Result text")
        )));

        String result = response.getToolResult();
        assertEquals("Result text", result);
    }

    @Test
    void testGetToolResultEmpty() {
        MCPResponse response = new MCPResponse();
        response.setResult(Map.of());

        String result = response.getToolResult();
        // 当 result 是空 Map 时，toString() 返回 "{}"
        assertEquals("{}", result);
    }

    @Test
    void testSerialization() throws Exception {
        MCPResponse response = new MCPResponse();
        response.setId(1L);
        response.setResult(Map.of("tools", List.of()));

        String json = objectMapper.writeValueAsString(response);
        assertNotNull(json);
        assertTrue(json.contains("\"jsonrpc\":\"2.0\""));
    }
}