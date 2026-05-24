package com.ruc.platform.ai.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiConfigSaveDTO {
    private String configName;
    private String provider;
    private String baseUrl;
    private String apiKey;
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
}
