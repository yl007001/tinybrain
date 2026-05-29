package com.tinybrain.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置
 * <p>
 * - 分页插件（自动识别数据库类型）
 * - 自动填充处理器（createTime / updateTime）
 * <p>
 * 面试重点：
 * 1. PaginationInnerInterceptor 原理：拦截 SQL，自动拼接 COUNT + LIMIT
 * 2. MetaObjectHandler 在 insert/update 时自动注入字段值
 * 3. 分页插件需要注册为 @Bean，且只能有一个拦截器实例
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * MyBatis-Plus 拦截器（分页 + 其他插件）
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 自动填充处理器
     * insert 时自动填充 createTime、updateTime
     * update 时自动填充 updateTime
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
