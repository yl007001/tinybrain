package com.tinybrain.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 批量上传单条文档项
 */
@Data
public class BatchUploadItem {

    @NotBlank(message = "文档标题不能为空")
    private String title;

    @NotBlank(message = "文档内容不能为空")
    private String content;

    /** 内容类型：markdown / text / html */
    private String contentType = "markdown";
}
