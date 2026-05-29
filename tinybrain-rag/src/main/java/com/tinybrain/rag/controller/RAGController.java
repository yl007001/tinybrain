package com.tinybrain.rag.controller;

import com.tinybrain.common.constant.CommonConstant;
import com.tinybrain.common.response.R;
import com.tinybrain.rag.dto.RAGResult;
import com.tinybrain.rag.service.RAGService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * RAG 检索控制器
 * <p>
 * 提供文档索引和智能问答接口。
 */
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RAGController {

    private final RAGService ragService;

    /**
     * 索引文档（分块 + 向量化 + 存储）
     */
    @PostMapping("/index/{documentId}")
    public R<Void> indexDocument(@PathVariable Long documentId) {
        ragService.indexDocument(documentId);
        return R.ok("文档索引完成");
    }

    /**
     * RAG 问答
     *
     * @param question 用户问题
     * @param topK     检索 Top-K（默认 5）
     */
    @GetMapping("/ask")
    public R<RAGResult> ask(@RequestParam String question,
                            @RequestParam(defaultValue = "5") int topK) {
        if (question.isBlank()) {
            return R.fail(400, "问题不能为空");
        }
        RAGResult result = ragService.ask(question, Math.min(topK, 20));
        return R.ok(result);
    }
}
