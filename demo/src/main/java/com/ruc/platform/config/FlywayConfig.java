package com.ruc.platform.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

/**
 * Flyway 配置类
 * 人大金仓兼容PostgreSQL协议，需要自定义配置
 */
@Configuration
public class FlywayConfig {

    @Autowired
    private Environment environment;

    /**
     * 自定义Flyway迁移策略
     * 强制使用PostgreSQL方言适配人大金仓
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            boolean isH2Profile = Arrays.stream(environment.getActiveProfiles())
                    .anyMatch(p -> "h2".equalsIgnoreCase(p));
            String url = environment.getProperty("spring.datasource.url", "");
            boolean isH2Url = url != null && url.toLowerCase().startsWith("jdbc:h2:");
            if (isH2Profile || isH2Url) {
                flyway.repair();
            }
            flyway.migrate();
        };
    }
}
