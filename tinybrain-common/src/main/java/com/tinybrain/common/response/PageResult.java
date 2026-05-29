package com.tinybrain.common.response;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 通用分页响应
 * <p>
 * 基于 MyBatis-Plus Page 对象封装，统一分页格式。
 *
 * @param <T> 列表数据类型
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页码 */
    private long page;
    /** 每页大小 */
    private long pageSize;
    /** 总记录数 */
    private long total;
    /** 总页数 */
    private long pages;
    /** 数据列表 */
    private List<T> records;

    public static <T> PageResult<T> empty() {
        PageResult<T> result = new PageResult<>();
        result.setPage(1);
        result.setPageSize(10);
        result.setTotal(0);
        result.setPages(0);
        result.setRecords(Collections.emptyList());
        return result;
    }

    public static <T> PageResult<T> of(long page, long pageSize, long total, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.setPage(page);
        result.setPageSize(pageSize);
        result.setTotal(total);
        result.setPages((total + pageSize - 1) / pageSize);
        result.setRecords(records);
        return result;
    }
}
