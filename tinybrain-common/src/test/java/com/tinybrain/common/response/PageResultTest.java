package com.tinybrain.common.response;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分页结果 PageResult 单元测试
 */
class PageResultTest {

    @Test
    void of_shouldCreatePageResult() {
        List<String> records = List.of("a", "b", "c");
        PageResult<String> result = PageResult.of(1L, 10L, 3L, records);

        assertEquals(1, result.getPage());
        assertEquals(10, result.getPageSize());
        assertEquals(3, result.getTotal());
        assertEquals(1, result.getPages()); // 3/10 = 1 页
        assertEquals(records, result.getRecords());
    }

    @Test
    void of_shouldCalculatePagesCorrectly() {
        PageResult<String> result = PageResult.of(1L, 10L, 25L, List.of());

        assertEquals(3, result.getPages()); // 25/10 = 3 页
    }

    @Test
    void of_shouldHandleEmptyRecords() {
        PageResult<String> result = PageResult.of(1L, 10L, 0L, List.of());

        assertEquals(0, result.getTotal());
        assertEquals(0, result.getPages());
        assertTrue(result.getRecords().isEmpty());
    }
}
