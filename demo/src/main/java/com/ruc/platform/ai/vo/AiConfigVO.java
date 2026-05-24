package com.ruc.platform.ai.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AiConfigVO {
    private Long id;
    private String configName;
    private String provider;
    private String baseUrl;
    private String apiKeyMask;
    private Boolean hasApiKey;
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
