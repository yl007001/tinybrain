package com.tinybrain.knowledge.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档响应 VO
 * <p>
 * 列表查询时 content 为 null（减少传输量），
 * 详情查询时 content 返回完整内容。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentVO {

    private Long id;
    private String title;
    private String summary;
    private String content;
    private String contentType;
    private Integer status;
    private List<String> tags;
    private Long userId;
    private String creatorName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
