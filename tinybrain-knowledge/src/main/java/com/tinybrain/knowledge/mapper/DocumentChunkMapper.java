package com.tinybrain.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tinybrain.knowledge.entity.DocumentChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文档分块 Mapper
 */
@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {

    /**
     * 根据文档ID获取所有分块（按序号排序）
     */
    List<DocumentChunk> selectByDocumentIdOrderByIndex(@Param("documentId") Long documentId);

    /**
     * 批量删除文档的所有分块
     */
    int deleteByDocumentId(@Param("documentId") Long documentId);
}
