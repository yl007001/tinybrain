package com.tinybrain.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 业务异常单元测试
 */
class BusinessExceptionTest {

    @Test
    void notFound_shouldCreate404Exception() {
        BusinessException e = BusinessException.notFound("文档不存在");

        assertEquals(404, e.getCode());
        assertEquals("文档不存在", e.getMessage());
    }

    @Test
    void unauthorized_shouldCreate401Exception() {
        BusinessException e = BusinessException.unauthorized("请先登录");

        assertEquals(401, e.getCode());
        assertEquals("请先登录", e.getMessage());
    }

    @Test
    void forbidden_shouldCreate403Exception() {
        BusinessException e = BusinessException.forbidden("无权访问");

        assertEquals(403, e.getCode());
        assertEquals("无权访问", e.getMessage());
    }

    @Test
    void badRequest_shouldCreate400Exception() {
        BusinessException e = BusinessException.badRequest("参数错误");

        assertEquals(400, e.getCode());
        assertEquals("参数错误", e.getMessage());
    }

    @Test
    void conflict_shouldCreate409Exception() {
        BusinessException e = BusinessException.conflict("资源冲突");

        assertEquals(409, e.getCode());
        assertEquals("资源冲突", e.getMessage());
    }

    @Test
    void defaultConstructor_shouldUse500Code() {
        BusinessException e = new BusinessException("默认错误");

        assertEquals(500, e.getCode());
        assertEquals("默认错误", e.getMessage());
    }

    @Test
    void constructorWithCause_shouldPreserveCause() {
        Throwable cause = new RuntimeException("原始异常");
        BusinessException e = new BusinessException(503, "服务不可用", cause);

        assertEquals(503, e.getCode());
        assertEquals("服务不可用", e.getMessage());
        assertNotNull(e.getCause());
        assertEquals("原始异常", e.getCause().getMessage());
    }
}
