package com.tinybrain.agent.plugin.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 日期时间工具测试
 */
class DateTimeToolTest {

    private DateTimeTool tool;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        tool = new DateTimeTool();
        mapper = new ObjectMapper();
    }

    @Test
    void getName_shouldReturnGetDatetime() {
        assertEquals("get_datetime", tool.getName());
    }

    @Test
    void getDescription_shouldReturnDescription() {
        assertNotNull(tool.getDescription());
    }

    @Test
    void getParametersSchema_shouldReturnValidSchema() {
        ObjectNode schema = tool.getParametersSchema();
        assertNotNull(schema);
        assertEquals("object", schema.get("type").asText());
    }

    @Test
    void execute_shouldReturnDateAndTime() throws Exception {
        ObjectNode args = mapper.createObjectNode();
        String result = tool.execute(args, mapper);
        assertNotNull(result);
        assertTrue(result.contains("Date:"));
        assertTrue(result.contains("Time:"));
        assertTrue(result.matches(".*\\d{4}-\\d{2}-\\d{2}.*"));
        assertTrue(result.matches(".*\\d{2}:\\d{2}:\\d{2}.*"));
    }
}
