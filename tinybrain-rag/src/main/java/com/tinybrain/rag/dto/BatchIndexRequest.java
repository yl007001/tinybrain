package com.tinybrain.rag.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量索引请求
 */
@Data
public class BatchIndexRequest {

    @NotEmpty(message = "文档 ID 列表不能为空")
    private List<Long> ids;
}
