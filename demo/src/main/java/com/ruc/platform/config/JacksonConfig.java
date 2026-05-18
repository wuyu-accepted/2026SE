package com.ruc.platform.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 配置类
 * 配置日期时间序列化/反序列化格式
 */
@Configuration
public class JacksonConfig {

    /**
     * 自定义Jackson日期时间格式化
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            // 日期时间序列化/反序列化器
            LocalDateTimeDeserializer localDateTimeDeserializer = new LocalDateTimeDeserializer(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            LocalDateTimeSerializer localDateTimeSerializer = new LocalDateTimeSerializer(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            // 日期序列化/反序列化器
            LocalDateDeserializer localDateDeserializer = new LocalDateDeserializer(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalDateSerializer localDateSerializer = new LocalDateSerializer(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            builder.deserializers(localDateTimeDeserializer, localDateDeserializer);
            builder.serializers(localDateTimeSerializer, localDateSerializer);

            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        };
    }
}
