package com.tinybrain.knowledge.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tinybrain.common.exception.BusinessException;
import com.tinybrain.common.response.PageResult;
import com.tinybrain.knowledge.dto.*;
import com.tinybrain.knowledge.entity.Document;
import com.tinybrain.knowledge.mapper.DocumentMapper;
import com.tinybrain.knowledge.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档服务实现
 * <p>
 * 事务管理：创建/更新/删除均需要事务保证数据一致性。
 * 后续 Phase 2 在此加入 RAG 相关的分块 + 向量化逻辑。
 */
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, Document> implements DocumentService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentVO create(DocumentCreateRequest request, Long userId) {
        Document doc = new Document();
        doc.setTitle(request.getTitle());
        doc.setSummary(request.getSummary());
        doc.setContent(request.getContent());
        doc.setContentType(request.getContentType());
        doc.setStatus(1); // 默认已发布
        doc.setTags(request.getTags() != null ? JSONUtil.toJsonStr(request.getTags()) : null);
        doc.setUserId(userId);

        save(doc);
        return toVO(doc);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentVO update(Long id, DocumentUpdateRequest request, Long userId) {
        Document doc = getById(id);
        if (doc == null) {
            throw BusinessException.notFound("文档不存在");
        }
        if (!doc.getUserId().equals(userId)) {
            throw BusinessException.forbidden("无权修改此文档");
        }

        if (request.getTitle() != null) doc.setTitle(request.getTitle());
        if (request.getSummary() != null) doc.setSummary(request.getSummary());
        if (request.getContent() != null) doc.setContent(request.getContent());
        if (request.getContentType() != null) doc.setContentType(request.getContentType());
        if (request.getStatus() != null) doc.setStatus(request.getStatus());
        if (request.getTags() != null) doc.setTags(JSONUtil.toJsonStr(request.getTags()));

        updateById(doc);
        return toVO(doc);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long userId) {
        Document doc = getById(id);
        if (doc == null) {
            throw BusinessException.notFound("文档不存在");
        }
        if (!doc.getUserId().equals(userId) && !"ROLE_ADMIN".equals(
                com.tinybrain.common.util.SecurityUtil.getCurrentUserRole())) {
            throw BusinessException.forbidden("无权删除此文档");
        }
        removeById(id);
    }

    @Override
    public DocumentVO getDetail(Long id) {
        Document doc = getById(id);
        if (doc == null) {
            throw BusinessException.notFound("文档不存在");
        }
        return toVO(doc);
    }

    @Override
    public PageResult<DocumentVO> queryPage(DocumentQueryRequest request, Long userId) {
        Page<Document> page = new Page<>(request.getPage(), request.getPageSize());

        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<Document>()
                .eq(Document::getUserId, userId)
                .eq(Document::getDeleted, 0)
                .like(request.getKeyword() != null, Document::getTitle, request.getKeyword())
                .eq(request.getStatus() != null, Document::getStatus, request.getStatus())
                .orderByDesc(Document::getUpdateTime);

        IPage<Document> result = page(page, wrapper);

        List<DocumentVO> voList = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), voList);
    }

    private DocumentVO toVO(Document doc) {
        DocumentVO vo = new DocumentVO();
        vo.setId(doc.getId());
        vo.setTitle(doc.getTitle());
        vo.setSummary(doc.getSummary());
        vo.setContentType(doc.getContentType());
        vo.setStatus(doc.getStatus());
        vo.setUserId(doc.getUserId());
        vo.setCreateTime(doc.getCreateTime());
        vo.setUpdateTime(doc.getUpdateTime());

        // tags JSON 字符串转 List
        if (doc.getTags() != null) {
            vo.setTags(JSONUtil.parseArray(doc.getTags()).toList(String.class));
        }

        return vo;
    }
}
