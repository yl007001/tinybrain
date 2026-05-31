package com.tinybrain.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 异步文档索引服务
 * <p>
 * 文档索引（分块 → 向量化 → 存储）是耗时操作，
 * 通过 @Async 在后台线程池执行，不阻塞 HTTP 请求。
 * <p>
 * 面试重点：
 * 1. 异步处理 vs 同步处理的选择
 * 2. 线程池隔离：索引任务不占用 HTTP 线程
 * 3. 失败重试：索引失败需要有重试机制
 * 4. 事件驱动：可扩展为 MQ 模式
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIndexingService {

    private final RAGService ragService;

    /**
     * 异步索引文档
     * <p>
     * 调用后立即返回，索引在后台线程池执行。
     * 如果索引失败，会在日志中记录错误。
     *
     * @param documentId 文档ID
     * @param userId     用户ID（用于日志追踪）
     */
    @Async("taskExecutor")
    public void indexDocumentAsync(Long documentId, Long userId) {
        long start = System.currentTimeMillis();
        log.info("开始异步索引文档: documentId={}, userId={}", documentId, userId);
        try {
            ragService.indexDocument(documentId);
            long elapsed = System.currentTimeMillis() - start;
            log.info("异步索引完成: documentId={}, 耗时={}ms", documentId, elapsed);
        } catch (Exception e) {
            log.error("异步索引失败: documentId={}, error={}", documentId, e.getMessage(), e);
        }
    }
}
