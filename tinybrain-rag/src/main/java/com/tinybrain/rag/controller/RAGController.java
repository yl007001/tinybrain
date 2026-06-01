package com.tinybrain.rag.controller;

import com.tinybrain.common.response.R;
import com.tinybrain.common.util.SecurityUtil;
import com.tinybrain.rag.dto.RAGResult;
import com.tinybrain.rag.service.DocumentIndexingService;
import com.tinybrain.rag.service.RAGService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * RAG 检索控制器
 * <p>
 * 提供文档索引（异步）和智能问答接口。
 */
@Tag(name = "03-RAG 检索", description = "文档索引（分块→向量化→存储）、RAG 智能问答、检索统计")
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RAGController {

    private final RAGService ragService;
    private final DocumentIndexingService indexingService;

    @Operation(summary = "索引文档", description = "将文档分块、向量化后存入向量库（异步执行，立即返回）")
    @PostMapping("/index/{documentId}")
    public R<Void> indexDocument(@PathVariable Long documentId) {
        Long userId = SecurityUtil.getCurrentUserId();
        // 异步执行索引，不阻塞 HTTP 线程
        indexingService.indexDocumentAsync(documentId, userId);
        return R.okMsg("文档索引任务已提交，将在后台异步执行");
    }

    @Operation(summary = "RAG 问答", description = "基于知识库内容进行智能问答：查询改写 → 向量检索 → LLM 增强生成")
    @GetMapping("/ask")
    public R<RAGResult> ask(@RequestParam String question,
                            @Parameter(description = "检索 Top-K 相关文档块", example = "5")
                            @RequestParam(defaultValue = "5") int topK) {
        if (question.isBlank()) {
            return R.fail(400, "问题不能为空");
        }
        RAGResult result = ragService.ask(question, Math.min(topK, 20));
        return R.ok(result);
    }

    @Operation(summary = "检索统计", description = "查看向量存储和 IVF 索引的统计信息")
    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        return R.ok(ragService.getStats());
    }
}
