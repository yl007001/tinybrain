package com.tinybrain.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tinybrain.common.response.PageResult;
import com.tinybrain.knowledge.dto.*;
import com.tinybrain.knowledge.entity.Document;

/**
 * 文档服务接口
 * <p>
 * 提供文档的 CRUD、搜索、分块管理等功能。
 * 后续 Phase 2 在此接口基础上扩展 RAG 检索。
 */
public interface DocumentService extends IService<Document> {

    /**
     * 创建文档
     */
    DocumentVO create(DocumentCreateRequest request, Long userId);

    /**
     * 更新文档
     */
    DocumentVO update(Long id, DocumentUpdateRequest request, Long userId);

    /**
     * 删除文档（逻辑删除）
     */
    void delete(Long id, Long userId);

    /**
     * 获取文档详情
     */
    DocumentVO getDetail(Long id);

    /**
     * 分页查询文档
     */
    PageResult<DocumentVO> queryPage(DocumentQueryRequest request, Long userId);
}
