package com.tinybrain.rag.listener;

import com.tinybrain.common.event.DocumentIndexEvent;
import com.tinybrain.rag.service.DocumentIndexingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 文档索引事件监听器
 * <p>
 * 监听 knowledge 模块发布的索引事件，在 rag 模块中执行异步索引。
 * 通过 Spring 事件机制实现跨模块解耦。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentIndexEventListener {

    private final DocumentIndexingService indexingService;

    /**
     * 处理文档批量索引事件（异步执行，不阻塞发布方线程）
     */
    @Async("taskExecutor")
    @EventListener
    public void handleDocumentIndexEvent(DocumentIndexEvent event) {
        log.info("收到文档索引事件: documentIds={}, userId={}, 数量={}",
                event.getDocumentIds(), event.getUserId(), event.getDocumentIds().size());

        for (Long documentId : event.getDocumentIds()) {
            try {
                indexingService.indexDocumentAsync(documentId, event.getUserId());
            } catch (Exception e) {
                log.error("提交索引任务失败: documentId={}, error={}", documentId, e.getMessage(), e);
            }
        }

        log.info("文档索引事件处理完成: 共提交 {} 个索引任务", event.getDocumentIds().size());
    }
}
