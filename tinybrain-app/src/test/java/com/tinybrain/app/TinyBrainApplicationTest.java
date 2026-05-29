package com.tinybrain.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * 应用启动测试
 * <p>
 * 验证 Spring 上下文能否正常加载，所有 Bean 是否可正确注入。
 */
@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration",
        "tinybrain.llm.api-key=test-key"
})
class TinyBrainApplicationTest {

    @Test
    void contextLoads() {
        // 验证 ApplicationContext 能正常启动
        // 未抛出异常即为通过
    }
}
