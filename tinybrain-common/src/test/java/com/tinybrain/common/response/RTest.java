package com.tinybrain.common.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 统一响应体 R 单元测试
 */
class RTest {

    @Test
    void ok_shouldReturnSuccessResponse() {
        R<String> result = R.ok("hello");

        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertEquals("hello", result.getData());
        assertTrue(result.isSuccess());
    }

    @Test
    void ok_withMessage_shouldReturnCustomMessage() {
        R<Integer> result = R.ok("自定义消息", 42);

        assertEquals(200, result.getCode());
        assertEquals("自定义消息", result.getMessage());
        assertEquals(42, result.getData());
    }

    @Test
    void ok_noData_shouldReturnNullData() {
        R<Void> result = R.ok();

        assertEquals(200, result.getCode());
        assertTrue(result.isSuccess());
        assertNull(result.getData());
    }

    @Test
    void fail_shouldReturnErrorResponse() {
        R<Void> result = R.fail("出错了");

        assertEquals(500, result.getCode());
        assertEquals("出错了", result.getMessage());
        assertFalse(result.isSuccess());
    }

    @Test
    void fail_withCode_shouldReturnCustomCode() {
        R<Void> result = R.fail(400, "参数错误");

        assertEquals(400, result.getCode());
        assertEquals("参数错误", result.getMessage());
        assertFalse(result.isSuccess());
    }

    @Test
    void jsonInclude_shouldOmitNullData() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        R<Void> result = R.ok();

        String json = mapper.writeValueAsString(result);
        assertTrue(json.contains("\"code\":200"));
        assertTrue(json.contains("\"message\":\"success\""));
    }
}
