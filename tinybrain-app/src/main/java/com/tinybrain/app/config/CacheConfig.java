package com.tinybrain.app.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存配置 — Redis 不可用时的降级方案
 * <p>
 * 当 Redis 未连接时，自动使用内存缓存（ConcurrentMapCacheManager）。
 * 保证 @Cacheable 注解在任何环境下都能正常工作。
 */
@Configuration
@EnableCaching
@ConditionalOnMissingBean(CacheManager.class)
public class CacheConfig {

    @Bean
    public CacheManager fallbackCacheManager() {
        return new ConcurrentMapCacheManager("users", "documents", "ragResults");
    }
}
