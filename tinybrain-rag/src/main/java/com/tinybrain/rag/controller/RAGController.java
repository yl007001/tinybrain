package com.tinybrain.rag.controller;

import com.tinybrain.common.constant.CommonConstant;
import com.tinybrain.common.response.R;
import com.tinybrain.rag.dto.RAGResult;
import com.tinybrain.rag.service.RAGService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * RAG 检索控制器
 * <p>
 * 提供文档索引和智能问答接口。
 */
@Tag(name = "03-RAG 检索", description = "文档索引（分块→向量化→存储）、RAG 智能问答")
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RAGController {

    private final RAGService ragService;

    @Operation(summary = "索引文档", description = "将文档分块、向量化后存入向量库，供后续 RAG 检索使用")
    @PostMapping("/index/{documentId}")
    public R<Void> indexDocument(@PathVariable Long documentId) {
        ragService.indexDocument(documentId);
        return R.okMsg("文档索引完成");
    }

    @Operation(summary = "RAG 问答", description = "基于知识库内容进行智能问答：向量检索 → 上下文拼接 → LLM 生成")
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
}
