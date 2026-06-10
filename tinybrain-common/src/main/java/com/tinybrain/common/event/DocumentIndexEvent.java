package com.tinybrain.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * 文档索引事件
 * <p>
 * 由 knowledge 模块发布，rag 模块监听并执行异步索引。
 * 通过 Spring 事件机制实现跨模块解耦调用。
 */
@Getter
public class DocumentIndexEvent extends ApplicationEvent {

    /** 需要索引的文档 ID 列表 */
    private final List<Long> documentIds;

    /** 触发索引的用户 ID */
    private final Long userId;

    public DocumentIndexEvent(Object source, List<Long> documentIds, Long userId) {
        super(source);
        this.documentIds = documentIds;
        this.userId = userId;
    }
}
