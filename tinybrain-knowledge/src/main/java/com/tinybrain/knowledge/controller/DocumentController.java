package com.tinybrain.knowledge.controller;

import com.tinybrain.common.response.PageResult;
import com.tinybrain.common.response.R;
import com.tinybrain.common.util.SecurityUtil;
import com.tinybrain.knowledge.dto.*;
import com.tinybrain.knowledge.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 文档控制器
 * <p>
 * 提供知识库文档的 CRUD 接口。
 * 所有接口需要 JWT 认证（由 Spring Security 控制）。
 */
@Slf4j
@Tag(name = "02-知识库文档", description = "文档的 CRUD、上传、分页查询")
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "创建文档", description = "创建新的知识库文档，支持 Markdown/纯文本格式")
    @PostMapping
    public R<DocumentVO> create(@Valid @RequestBody DocumentCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        DocumentVO doc = documentService.create(request, userId);
        return R.ok("创建成功", doc);
    }

    @Operation(summary = "更新文档", description = "更新文档标题、摘要、内容、标签等字段")
    @PutMapping("/{id}")
    public R<DocumentVO> update(@PathVariable Long id,
                                @Valid @RequestBody DocumentUpdateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        DocumentVO doc = documentService.update(id, request, userId);
        return R.ok("更新成功", doc);
    }

    @Operation(summary = "删除文档", description = "逻辑删除文档（标记 deleted=1）")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
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
    public R<PageResult<DocumentVO>> queryPage(DocumentQueryRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
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

    @Operation(summary = "上传文档文件", description = "上传 Markdown/文本文件作为知识库文档，自动读取文件内容并创建文档记录")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<DocumentVO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "markdown") String contentType) {
        Long userId = SecurityUtil.getCurrentUserId();

        if (file.isEmpty()) {
            return R.fail(400, "上传文件不能为空");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            filename = "unnamed.txt";
        }

        // 检查文件大小（最大 10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            return R.fail(400, "文件大小不能超过 10MB");
        }

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);

            DocumentCreateRequest request = new DocumentCreateRequest();
            // 去掉扩展名作为标题
            String title = filename.contains(".") ? filename.substring(0, filename.lastIndexOf('.')) : filename;
            request.setTitle(title);
            request.setContent(content);
            request.setContentType(contentType);
            request.setSummary(content.length() > 200 ? content.substring(0, 200) + "..." : content);

            DocumentVO doc = documentService.create(request, userId);
            log.info("文档上传成功: filename={}, userId={}, docId={}", filename, userId, doc.getId());
            return R.ok("上传成功", doc);
        } catch (IOException e) {
            log.error("文档上传失败: {}", e.getMessage());
            return R.fail(500, "文件读取失败: " + e.getMessage());
        }
    }
}
