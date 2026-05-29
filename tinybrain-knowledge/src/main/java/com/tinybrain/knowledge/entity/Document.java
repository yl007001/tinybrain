package com.tinybrain.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档实体
 * <p>
 * 对应表 kb_document，存储用户上传的各类文档。
 * 支持：标题搜索、内容全文检索、标签筛选、状态管理。
 * <p>
 * 内容存储说明：
 * - Phase 1：content 直接存文本（MySQL TEXT 类型）
 * - Phase 2：content 向量化后存向量库，同时保留原始文本
 * - 大文件支持：后续可迁移到 OSS，content 存文件链接
 */
@Data
@TableName("kb_document")
public class Document {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文档标题 */
    private String title;

    /** 文档摘要 */
    private String summary;

    /** 文档内容（Markdown / 纯文本） */
    private String content;

    /** 内容类型：markdown / text / html */
    private String contentType;

    /** 状态：0=草稿, 1=已发布, 2=已归档 */
    private Integer status;

    /** 标签（JSON数组，如 ["java", "spring-boot"]） */
    private String tags;

    /** 创建者ID */
    private Long userId;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ========== 非数据库字段 ==========

    /** 标签列表（由 tags JSON 转换而来） */
    @TableField(exist = false)
    private java.util.List<String> tagList;
}
