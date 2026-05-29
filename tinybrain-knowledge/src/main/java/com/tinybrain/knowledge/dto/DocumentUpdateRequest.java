package com.tinybrain.knowledge.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 更新文档请求
 */
@Data
public class DocumentUpdateRequest {

    @Size(max = 200, message = "标题长度不超过200字符")
    private String title;

    @Size(max = 1000, message = "摘要长度不超过1000字符")
    private String summary;

    private String content;

    /** 内容类型：markdown / text / html */
    private String contentType;

    /** 状态：0=草稿, 1=已发布, 2=已归档 */
    private Integer status;

    /** 标签列表 */
    private List<String> tags;
}
