package com.ruc.platform.ai.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ai_provider_config")
public class AiProviderConfig {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String configName;
    private String provider;
    private String baseUrl;
    private String apiKeyCipher;
    private String apiKeyMask;
    private String model;
    private BigDecimal temperature;
    private BigDecimal topP;
    private Integer maxTokens;
    private BigDecimal presencePenalty;
    private BigDecimal frequencyPenalty;
    private String responseFormat;
    private Integer timeoutSeconds;
    private Boolean streamEnabled;
    private Integer retrievalTopK;
    private Integer actionTopK;
    private String systemPrompt;
    private Boolean enabled;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}
