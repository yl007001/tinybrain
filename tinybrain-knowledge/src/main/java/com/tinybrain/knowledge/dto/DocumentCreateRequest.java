package com.tinybrain.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建文档请求
 */
@Data
public class DocumentCreateRequest {

    @NotBlank(message = "文档标题不能为空")
    @Size(max = 200, message = "标题长度不超过200字符")
    private String title;

    @Size(max = 1000, message = "摘要长度不超过1000字符")
    private String summary;

    @NotBlank(message = "文档内容不能为空")
    private String content;

    /** 内容类型：markdown / text / html */
    private String contentType = "markdown";

    /** 标签列表 */
    private List<String> tags;
}
