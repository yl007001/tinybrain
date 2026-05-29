package com.tinybrain.knowledge.controller;

import com.tinybrain.common.constant.CommonConstant;
import com.tinybrain.common.response.PageResult;
import com.tinybrain.common.response.R;
import com.tinybrain.knowledge.dto.*;
import com.tinybrain.knowledge.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "02-知识库文档", description = "文档的 CRUD、分页查询、管理员接口")
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "创建文档", description = "创建新的知识库文档，支持 Markdown/纯文本格式")
    @PostMapping
    public R<DocumentVO> create(@Valid @RequestBody DocumentCreateRequest request,
                                @Parameter(hidden = true) @RequestAttribute(CommonConstant.CURRENT_USER) Long userId) {
        DocumentVO doc = documentService.create(request, userId);
        return R.ok("创建成功", doc);
    }

    @Operation(summary = "更新文档", description = "更新文档标题、摘要、内容、标签等字段")
    @PutMapping("/{id}")
    public R<DocumentVO> update(@PathVariable Long id,
                                @Valid @RequestBody DocumentUpdateRequest request,
                                @Parameter(hidden = true) @RequestAttribute(CommonConstant.CURRENT_USER) Long userId) {
        DocumentVO doc = documentService.update(id, request, userId);
        return R.ok("更新成功", doc);
    }

    @Operation(summary = "删除文档", description = "逻辑删除文档（标记 deleted=1）")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id,
                          @Parameter(hidden = true) @RequestAttribute(CommonConstant.CURRENT_USER) Long userId) {
        documentService.delete(id, userId);
        return R.okMsg("删除成功");
    }

    @Operation(summary = "获取文档详情", description = "根据 ID 获取文档完整内容")
    @GetMapping("/{id}")
    public R<DocumentVO> getDetail(@PathVariable Long id) {
        DocumentVO doc = documentService.getDetail(id);
        return R.ok(doc);
    }

    @Operation(summary = "分页查询文档", description = "按关键词、状态分页查询当前用户的文档列表")
    @GetMapping
    public R<PageResult<DocumentVO>> queryPage(DocumentQueryRequest request,
                                                @Parameter(hidden = true) @RequestAttribute(CommonConstant.CURRENT_USER) Long userId) {
        PageResult<DocumentVO> result = documentService.queryPage(request, userId);
        return R.ok(result);
    }

    @Operation(summary = "管理员查询所有文档", description = "管理员查看所有用户的文档（需 ADMIN 角色）")
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public R<PageResult<DocumentVO>> adminQueryPage(DocumentQueryRequest request) {
        PageResult<DocumentVO> result = documentService.queryPage(request, null);
        return R.ok(result);
    }
}
