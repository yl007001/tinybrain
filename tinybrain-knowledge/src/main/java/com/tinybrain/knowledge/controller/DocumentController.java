package com.tinybrain.knowledge.controller;

import com.tinybrain.common.constant.CommonConstant;
import com.tinybrain.common.response.PageResult;
import com.tinybrain.common.response.R;
import com.tinybrain.knowledge.dto.*;
import com.tinybrain.knowledge.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 文档控制器
 * <p>
 * 提供知识库文档的 CRUD 接口。
 * 所有接口需要 JWT 认证（由 Spring Security 控制）。
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /**
     * 创建文档
     */
    @PostMapping
    public R<DocumentVO> create(@Valid @RequestBody DocumentCreateRequest request,
                                @RequestAttribute(CommonConstant.CURRENT_USER) Long userId) {
        DocumentVO doc = documentService.create(request, userId);
        return R.ok("创建成功", doc);
    }

    /**
     * 更新文档
     */
    @PutMapping("/{id}")
    public R<DocumentVO> update(@PathVariable Long id,
                                @Valid @RequestBody DocumentUpdateRequest request,
                                @RequestAttribute(CommonConstant.CURRENT_USER) Long userId) {
        DocumentVO doc = documentService.update(id, request, userId);
        return R.ok("更新成功", doc);
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id,
                          @RequestAttribute(CommonConstant.CURRENT_USER) Long userId) {
        documentService.delete(id, userId);
        return R.ok("删除成功");
    }

    /**
     * 获取文档详情
     */
    @GetMapping("/{id}")
    public R<DocumentVO> getDetail(@PathVariable Long id) {
        DocumentVO doc = documentService.getDetail(id);
        return R.ok(doc);
    }

    /**
     * 分页查询文档列表
     */
    @GetMapping
    public R<PageResult<DocumentVO>> queryPage(DocumentQueryRequest request,
                                                @RequestAttribute(CommonConstant.CURRENT_USER) Long userId) {
        PageResult<DocumentVO> result = documentService.queryPage(request, userId);
        return R.ok(result);
    }

    /**
     * 管理员接口示例：查询所有文档（不受用户ID限制）
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public R<PageResult<DocumentVO>> adminQueryPage(DocumentQueryRequest request) {
        // 管理员可以查看所有文档
        PageResult<DocumentVO> result = documentService.queryPage(request, null);
        return R.ok(result);
    }
}
