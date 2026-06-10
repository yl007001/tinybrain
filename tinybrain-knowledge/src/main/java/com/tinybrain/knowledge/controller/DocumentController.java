package com.tinybrain.knowledge.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tinybrain.common.event.DocumentIndexEvent;
import com.tinybrain.common.response.PageResult;
import com.tinybrain.common.response.R;
import com.tinybrain.common.util.SecurityUtil;
import com.tinybrain.knowledge.dto.*;
import com.tinybrain.knowledge.entity.Document;
import com.tinybrain.knowledge.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final ApplicationEventPublisher eventPublisher;

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

    // ========== 批量操作接口 ==========

    @Operation(summary = "批量上传文档", description = "批量创建文档，接收 JSON 数组，每个元素包含 title、content、contentType")
    @PostMapping("/batch-upload")
    public R<Map<String, Object>> batchUpload(@Valid @RequestBody List<BatchUploadItem> items) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<DocumentVO> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            BatchUploadItem item = items.get(i);
            try {
                DocumentCreateRequest request = new DocumentCreateRequest();
                request.setTitle(item.getTitle());
                request.setContent(item.getContent());
                request.setContentType(item.getContentType());
                request.setSummary(item.getContent().length() > 200
                        ? item.getContent().substring(0, 200) + "..." : item.getContent());

                DocumentVO doc = documentService.create(request, userId);
                results.add(doc);
            } catch (Exception e) {
                log.error("批量上传第 {} 项失败: title={}, error={}", i + 1, item.getTitle(), e.getMessage());
                errors.add("第 " + (i + 1) + " 项失败: " + item.getTitle() + " - " + e.getMessage());
            }
        }

        Map<String, Object> data = Map.of(
                "success", results,
                "errors", errors,
                "successCount", results.size(),
                "errorCount", errors.size()
        );

        if (errors.isEmpty()) {
            return R.ok("批量上传成功，共 " + results.size() + " 个文档", data);
        } else {
            return R.ok("部分上传成功: 成功 " + results.size() + " 个，失败 " + errors.size() + " 个", data);
        }
    }

    @Operation(summary = "批量删除文档", description = "根据 ID 列表批量逻辑删除文档")
    @PostMapping("/batch-delete")
    public R<Void> batchDelete(@Valid @RequestBody BatchIdsRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<Long> ids = request.getIds();

        // 校验文档归属
        List<Document> documents = documentService.listByIds(ids);
        List<Long> unauthorized = documents.stream()
                .filter(d -> !d.getUserId().equals(userId))
                .map(Document::getId)
                .collect(Collectors.toList());

        if (!unauthorized.isEmpty()) {
            return R.fail(403, "无权删除以下文档: " + unauthorized);
        }

        documentService.removeByIds(ids);
        log.info("批量删除文档: userId={}, ids={}, 数量={}", userId, ids, ids.size());
        return R.okMsg("批量删除成功，共 " + ids.size() + " 个文档");
    }

    @Operation(summary = "批量索引到 RAG", description = "根据 ID 列表批量将文档提交到 RAG 向量库进行索引（异步执行）")
    @PostMapping("/batch-index")
    public R<Void> batchIndex(@Valid @RequestBody BatchIdsRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<Long> ids = request.getIds();

        // 发布索引事件，由 rag 模块异步处理
        eventPublisher.publishEvent(new DocumentIndexEvent(this, ids, userId));
        log.info("批量索引任务已提交: userId={}, ids={}, 数量={}", userId, ids, ids.size());
        return R.okMsg("批量索引任务已提交，共 " + ids.size() + " 个文档将在后台异步执行");
    }

    @Operation(summary = "根据关键词批量索引", description = "查找标题或内容包含关键词的文档，批量提交 RAG 索引")
    @PostMapping("/batch-index-by-keyword")
    public R<Void> batchIndexByKeyword(@Valid @RequestBody BatchKeywordRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        String keyword = request.getKeyword();

        // 查询当前用户下标题或内容包含关键词的文档
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<Document>()
                .eq(Document::getUserId, userId)
                .and(w -> w
                        .like(Document::getTitle, keyword)
                        .or()
                        .like(Document::getContent, keyword));
        List<Document> documents = documentService.list(wrapper);

        if (documents.isEmpty()) {
            return R.okMsg("未找到包含关键词「" + keyword + "」的文档");
        }

        List<Long> ids = documents.stream().map(Document::getId).collect(Collectors.toList());

        // 发布索引事件
        eventPublisher.publishEvent(new DocumentIndexEvent(this, ids, userId));
        log.info("关键词批量索引任务已提交: userId={}, keyword={}, 匹配文档数={}", userId, keyword, ids.size());
        return R.okMsg("已找到 " + ids.size() + " 个匹配文档，索引任务已提交");
    }
}
