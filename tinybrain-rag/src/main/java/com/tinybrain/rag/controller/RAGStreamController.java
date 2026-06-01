package com.tinybrain.rag.controller;

import com.tinybrain.rag.dto.RAGResult;
import com.tinybrain.rag.service.RAGService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * RAG 流式控制器 — SSE (Server-Sent Events)
 * <p>
 * 相比传统 HTTP 轮询，SSE 支持服务器主动推送：
 * - 事件流按步骤推送（改写中 → 检索中 → 生成中 → 完成）
 * - 前端可以展示实时进度
 * - 连接由 HTTP 长连接维持
 * <p>
 * 前端接收示例：
 *   event: progress
 *   data: {"step": "rewriting", "message": "改写查询中..."}
 * <p>
 *   event: progress
 *   data: {"step": "searching", "message": "检索到 5 个相关文档块"}
 * <p>
 *   event: result
 *   data: {"answer": "...", "chunks": [...]}
 * <p>
 *   event: complete
 *   data: {}
 * <p>
 * ：
 * 1. SSE vs WebSocket vs 轮询的选型
 * 2. SSE 天然基于 HTTP，适合单向推送
 * 3. WebSocket 适合双向通信（Chat 场景）
 * 4. 连接管理与超时控制
 */
@Slf4j
@Tag(name = "03-RAG 检索（流式）", description = "SSE 流式 RAG 问答，支持实时进度推送")
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RAGStreamController {

    private final RAGService ragService;
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "sse-worker-");
        t.setDaemon(true);
        return t;
    });

    @PreDestroy
    public void destroy() {
        sseExecutor.shutdown();
        log.info("SSE 线程池已关闭");
    }

    @Operation(summary = "流式 RAG 问答 (SSE)", description = "基于 SSE 的流式 RAG 问答，实时推送处理进度和结果")
    @GetMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(
            @RequestParam String question,
            @RequestParam(defaultValue = "5") int topK) {

        // 超时时间：5 分钟
        SseEmitter emitter = new SseEmitter(300_000L);

        sseExecutor.execute(() -> {
            try {
                // Step 1: 发送开始事件
                sendEvent(emitter, "progress", Map.of("step", "start", "message", "开始处理问题..."));

                // Step 2: 执行 RAG 问答
                RAGResult result = ragService.ask(question, Math.min(topK, 20));

                // Step 3: 发送检索结果
                if (result.getChunks() != null && !result.getChunks().isEmpty()) {
                    sendEvent(emitter, "progress", Map.of(
                            "step", "searching",
                            "message", "检索到 " + result.getChunks().size() + " 个相关文档块"
                    ));
                    // 发送 chunks 信息
                    sendEvent(emitter, "chunks", Map.of(
                            "chunks", result.getChunks().stream()
                                    .map(c -> Map.of(
                                            "title", c.getDocumentTitle(),
                                            "score", c.getScore()
                                    ))
                                    .toList()
                    ));
                } else {
                    sendEvent(emitter, "progress", Map.of(
                            "step", "searching",
                            "message", "未检索到相关知识"
                    ));
                }

                // Step 4: 发送最终回答
                sendEvent(emitter, "result", Map.of(
                        "answer", result.getAnswer() != null ? result.getAnswer() : "无法生成回答",
                        "question", result.getQuestion(),
                        "tokens", result.getTotalTokens()
                ));

                // Step 5: 完成
                sendEvent(emitter, "complete", Map.of("status", "ok"));

                emitter.complete();
            } catch (Exception e) {
                log.error("SSE 流式处理失败: {}", e.getMessage());
                try {
                    sendEvent(emitter, "error", Map.of("message", "处理失败: " + e.getMessage()));
                    emitter.complete();
                } catch (IOException ignored) {}
            }
        });

        emitter.onCompletion(() -> log.debug("SSE 连接完成"));
        emitter.onTimeout(() -> log.warn("SSE 连接超时"));
        emitter.onError(e -> log.error("SSE 连接错误: {}", e.getMessage()));

        return emitter;
    }

    private void sendEvent(SseEmitter emitter, String event, Object data) throws IOException {
        emitter.send(SseEmitter.event()
                .name(event)
                .data(data, MediaType.APPLICATION_JSON));
    }
}
