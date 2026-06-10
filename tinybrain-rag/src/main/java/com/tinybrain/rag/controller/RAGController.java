package com.tinybrain.rag.controller;

import com.tinybrain.common.entity.Message;
import com.tinybrain.common.entity.Session;
import com.tinybrain.common.mapper.MessageMapper;
import com.tinybrain.common.mapper.SessionMapper;
import com.tinybrain.common.response.R;
import com.tinybrain.common.util.SecurityUtil;
import com.tinybrain.rag.dto.BatchIndexRequest;
import com.tinybrain.rag.dto.RAGResult;
import com.tinybrain.rag.service.DocumentIndexingService;
import com.tinybrain.rag.service.RAGService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    private final SessionMapper sessionMapper;
    private final MessageMapper messageMapper;

    @Operation(summary = "索引文档", description = "将文档分块、向量化后存入向量库（异步执行，立即返回）")
    @PostMapping("/index/{documentId}")
    public R<Void> indexDocument(@PathVariable Long documentId) {
        Long userId = SecurityUtil.getCurrentUserId();
        // 异步执行索引，不阻塞 HTTP 线程
        indexingService.indexDocumentAsync(documentId, userId);
        return R.okMsg("文档索引任务已提交，将在后台异步执行");
    }

    @Operation(summary = "批量索引文档", description = "根据 ID 列表批量将文档提交到向量库索引（异步执行，立即返回）")
    @PostMapping("/batch-index")
    public R<Void> batchIndex(@Valid @RequestBody BatchIndexRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<Long> ids = request.getIds();

        for (Long documentId : ids) {
            indexingService.indexDocumentAsync(documentId, userId);
        }

        return R.okMsg("批量索引任务已提交，共 " + ids.size() + " 个文档将在后台异步执行");
    }

    @Operation(summary = "RAG 问答", description = "基于知识库内容进行智能问答：查询改写 → 向量检索 → LLM 增强生成")
    @GetMapping("/ask")
    public R<RAGResult> ask(@RequestParam String question,
                            @Parameter(description = "检索 Top-K 相关文档块", example = "5")
                            @RequestParam(defaultValue = "5") int topK,
                            @Parameter(description = "会话ID（可选，为空则自动创建新会话）")
                            @RequestParam(required = false) String sessionId) {
        if (question.isBlank()) {
            return R.fail(400, "问题不能为空");
        }

        Long userId = SecurityUtil.getCurrentUserId();

        // 处理 sessionId：如果为空则创建新会话
        if (sessionId == null || sessionId.isBlank()) {
            Session session = new Session();
            session.setSessionId(UUID.randomUUID().toString());
            session.setUserId(userId);
            session.setType("rag");
            String title = question.length() <= 20 ? question : question.substring(0, 20);
            session.setTitle(title);
            sessionMapper.insert(session);
            sessionId = session.getSessionId();
        }

        RAGResult result = ragService.ask(question, Math.min(topK, 20));
        result.setSessionId(sessionId);

        // 保存用户问题到 ai_message
        Message userMsg = new Message();
        userMsg.setSessionId(sessionId);
        userMsg.setUserId(userId);
        userMsg.setRole("user");
        userMsg.setContent(question);
        messageMapper.insert(userMsg);

        // 保存 AI 回答到 ai_message
        Message assistantMsg = new Message();
        assistantMsg.setSessionId(sessionId);
        assistantMsg.setUserId(userId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(result.getAnswer());
        messageMapper.insert(assistantMsg);

        return R.ok(result);
    }

    @Operation(summary = "检索统计", description = "查看向量存储和 IVF 索引的统计信息")
    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        return R.ok(ragService.getStats());
    }
}
