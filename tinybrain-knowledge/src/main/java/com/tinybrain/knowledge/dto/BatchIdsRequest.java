package com.tinybrain.knowledge.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量操作请求（按 ID 列表）
 */
@Data
public class BatchIdsRequest {

    @NotEmpty(message = "ID 列表不能为空")
    private List<Long> ids;
}
