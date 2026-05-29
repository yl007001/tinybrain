package com.tinybrain.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 全局配置
 * <p>
 * - 统一日期时间格式为 "yyyy-MM-dd HH:mm:ss"
 * - 注册 JavaTimeModule 支持 Java 8 时间类型
 * - 禁止将日期序列化为时间戳（默认不开启）
 */
@Configuration
public class JacksonConfig {

    private static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        JavaTimeModule module = new JavaTimeModule();

        LocalDateTimeSerializer serializer = new LocalDateTimeSerializer(
                DateTimeFormatter.ofPattern(DATETIME_PATTERN));
        LocalDateTimeDeserializer deserializer = new LocalDateTimeDeserializer(
                DateTimeFormatter.ofPattern(DATETIME_PATTERN));

        module.addSerializer(LocalDateTime.class, serializer);
        module.addDeserializer(LocalDateTime.class, deserializer);

        mapper.registerModule(module);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }
}
