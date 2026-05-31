package com.tinybrain.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务线程池配置
 * <p>
 * 用于 @Async 异步执行耗时操作（如文档索引、批量嵌入等）。
 * <p>
 * ：
 * 1. ThreadPoolTaskExecutor vs ThreadPoolExecutor
 * 2. 核心线程数 / 最大线程数 / 队列容量的调优
 * 3. 拒绝策略：CallerRunsPolicy 保证任务不丢失
 * 4. 异步任务的异常处理
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /** 核心线程数 = CPU 核心数 */
    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    /** 最大线程数 = 核心数 × 2 */
    private static final int MAX_POOL_SIZE = CORE_POOL_SIZE * 2;

    /** 队列容量 */
    private static final int QUEUE_CAPACITY = 100;

    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("async-");
        // CallerRunsPolicy：当队列满时，由调用线程执行（不会丢任务）
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("异步任务线程池初始化: core={}, max={}, queue={}", CORE_POOL_SIZE, MAX_POOL_SIZE, QUEUE_CAPACITY);
        return executor;
    }
}
