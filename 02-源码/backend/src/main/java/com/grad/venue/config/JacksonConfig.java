package com.grad.venue.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 统一时间格式：全系统都用 yyyy-MM-dd HH:mm:ss，前后端一致，
 * 避免 LocalDateTime 默认 ISO 格式导致前端提交解析失败。
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeCustomizer() {
        return builder -> {
            SimpleModule m = new SimpleModule();
            m.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(FMT));
            m.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(FMT));
            builder.modules(m);
        };
    }
}
