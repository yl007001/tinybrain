package com.tinybrain.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 3.0 配置
 *
 * <p>统一 API 文档配置，包含：
 * <ul>
 *   <li>文档基本信息（标题、版本、联系方式）</li>
 *   <li>JWT Bearer Token 安全方案（全局自动添加）</li>
 *   <li>外部文档链接</li>
 * </ul>
 *
 * <p>访问地址：
 * <ul>
 *   <li>Swagger UI: http://localhost:8080/swagger-ui.html</li>
 *   <li>API 文档: http://localhost:8080/v3/api-docs</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tinyBrainOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TinyBrain API")
                        .description("AI Knowledge Engine - Knowledge Management, RAG Search, Agent Chat")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("TinyBrain Team")
                                .url("https://gitee.com/lisusuyeye/personal-ai-knowledge-engine"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
