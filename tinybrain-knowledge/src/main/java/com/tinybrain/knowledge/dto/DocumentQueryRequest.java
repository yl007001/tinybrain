package com.tinybrain.knowledge.dto;

import lombok.Data;

import java.util.List;

/**
 * 文档查询请求（分页+筛选）
 */
@Data
public class DocumentQueryRequest {

    /** 当前页码 */
    private long page = 1;

    /** 每页大小 */
    private long pageSize = 10;

    /** 搜索关键词 */
    private String keyword;

    /** 标签筛选 */
    private List<String> tags;

    /** 状态筛选 */
    private Integer status;
}
