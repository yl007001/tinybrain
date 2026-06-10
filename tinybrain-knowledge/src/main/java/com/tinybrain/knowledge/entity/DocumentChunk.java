package com.tinybrain.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档分块实体
 * <p>
 * 对应表 kb_document_chunk，将长文档切分为语义块。
 * 用途：
 * - Phase 2：每个 chunk 生成 Embedding 向量，存入向量库做 RAG
 * - 分块策略影响检索精度，是 RAG 系统的核心优化点
 * <p>
 * ：
 * 1. 分块粒度：太小丢语义，太大超 Token 限制
 * 2. 重叠策略：相邻块保留部分重叠，防止边界断句
 * 3. 结构化分块：Markdown 按标题分块，代码按函数分块
 */
@Data
@TableName("kb_document_chunk")
public class DocumentChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属文档ID */
    private Long documentId;

    /** 块序号 */
    private Integer chunkIndex;

    /** 块内容 */
    private String content;

    /** 向量嵌入（Phase 2 使用，二进制存储） */
    private byte[] embedding;

    /** LLM 提取的关键词（逗号分隔，用于关键词检索） */
    private String keywords;

    /** 块元数据（JSON，如起始位置、所属章节标题等） */
    private String chunkMeta;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
