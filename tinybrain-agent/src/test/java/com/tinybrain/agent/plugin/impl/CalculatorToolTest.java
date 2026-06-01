package com.tinybrain.agent.plugin.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 计算器工具测试 — 覆盖递归下降解析器
 */
class CalculatorToolTest {

    private CalculatorTool tool;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        tool = new CalculatorTool();
        mapper = new ObjectMapper();
    }

    @Test
    void getName_shouldReturnCalculator() {
        assertEquals("calculator", tool.getName());
    }

    @Test
    void getDescription_shouldReturnDescription() {
        assertNotNull(tool.getDescription());
        assertFalse(tool.getDescription().isEmpty());
    }

    @Test
    void getParametersSchema_shouldReturnValidSchema() {
        ObjectNode schema = tool.getParametersSchema();
        assertNotNull(schema);
        assertEquals("object", schema.get("type").asText());
        assertTrue(schema.has("properties"));
    }

    // ==================== 基本运算 ====================

    @Test
    void execute_simpleAddition() throws Exception {
        ObjectNode args = mapper.createObjectNode();
        args.put("expression", "2 + 3");
        String result = tool.execute(args, mapper);
        assertEquals("2 + 3 = 5", result);
    }

    @Test
    void execute_multiplication() throws Exception {
        ObjectNode args = mapper.createObjectNode();
        args.put("expression", "4 * 5");
        String result = tool.execute(args, mapper);
        assertEquals("4 * 5 = 20", result);
    }

    @Test
    void execute_division() throws Exception {
        ObjectNode args = mapper.createObjectNode();
        args.put("expression", "10 / 3");
        String result = tool.execute(args, mapper);
        assertTrue(result.contains("3.3333"));
    }

    @Test
    void execute_subtraction() throws Exception {
        ObjectNode args = mapper.createObjectNode();
        args.put("expression", "100 - 42");
        String result = tool.execute(args, mapper);
        assertEquals("100 - 42 = 58", result);
    }

    // ==================== 括号 ====================

    @Test
    void execute_parentheses() throws Exception {
        ObjectNode args = mapper.createObjectNode();
        args.put("expression", "(15 + 3) * 2");
        String result = tool.execute(args, mapper);
        assertEquals("(15 + 3) * 2 = 36", result);
    }

    @Test
    void execute_nestedParentheses() throws Exception {
        ObjectNode args = mapper.createObjectNode();
        args.put("expression", "((2 + 3) * (4 - 1))");
        String result = tool.execute(args, mapper);
        assertEquals("((2 + 3) * (4 - 1)) = 15", result);
    }

    // ==================== 幂运算 ====================

    @Test
    void execute_power() throws Exception {
        ObjectNode args = mapper.createObjectNode();
        args.put("expression", "2^10");
        String result = tool.execute(args, mapper);
        assertEquals("2^10 = 1024", result);
    }

    // ==================== 函数 ====================

    @Test
    void execute_sqrt() throws Exception {
        ObjectNode args = mapper.createObjectNode();
        args.put("expression", "sqrt(144)");
        String result = tool.execute(args, mapper);
        assertEquals("sqrt(144) = 12", result);
    }

    @Test
    void execute_sin() throws Exception {
        ObjectNode args = mapper.createObjectNode();
        args.put("expression", "sin(0)");
        String result = tool.execute(args, mapper);
        assertTrue(result.contains("= 0"));
    }

    @Test
    void execute_abs() throws Exception {
        ObjectNode args = mapper.createObjectNode();
        args.put("expression", "abs(-42)");
        String result = tool.execute(args, mapper);
        assertEquals("abs(-42) = 42", result);
    }

    // ==================== 常量 ====================

    @Test
    void execute_pi() throws Exception {
        ObjectNode args = mapper.createObjectNode();
        args.put("expression", "pi");
        String result = tool.execute(args, mapper);
        assertTrue(result.contains("3.14159"));
    }

    // ==================== 错误处理 ====================

    @Test
    void execute_emptyExpression() throws Exception {
        ObjectNode args = mapper.createObjectNode();
        args.put("expression", "");
        String result = tool.execute(args, mapper);
        assertTrue(result.contains("provide"));
    }

    @Test
    void execute_blankExpression() throws Exception {
        ObjectNode args = mapper.createObjectNode();
        args.put("expression", "   ");
        String result = tool.execute(args, mapper);
        assertTrue(result.contains("provide"));
    }

    @Test
    void execute_divisionByZero() throws Exception {
        ObjectNode args = mapper.createObjectNode();
        args.put("expression", "1/0");
        String result = tool.execute(args, mapper);
        assertTrue(result.contains("Failed") || result.contains("Error"));
    }

    @Test
    void execute_invalidExpression() throws Exception {
        ObjectNode args = mapper.createObjectNode();
        args.put("expression", "+++");
        String result = tool.execute(args, mapper);
        assertTrue(result.contains("Failed") || result.contains("Error"));
    }

    // ==================== 复杂表达式 ====================

    @Test
    void execute_complexExpression() throws Exception {
        ObjectNode args = mapper.createObjectNode();
        args.put("expression", "sqrt(16) + 2^3 - 1");
        String result = tool.execute(args, mapper);
        assertEquals("sqrt(16) + 2^3 - 1 = 11", result);
    }

    @Test
    void execute_unaryNegative() throws Exception {
        ObjectNode args = mapper.createObjectNode();
        args.put("expression", "-5 + 3");
        String result = tool.execute(args, mapper);
        assertEquals("-5 + 3 = -2", result);
    }
}
