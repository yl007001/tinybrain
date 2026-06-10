package com.tinybrain.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 按关键词批量操作请求
 */
@Data
public class BatchKeywordRequest {

    @NotBlank(message = "关键词不能为空")
    private String keyword;
}
