package com.ruc.platform.ai.vo;

import lombok.Data;

@Data
public class AiConfigTestVO {
    private Boolean success;
    private String answer;
    private Integer latencyMs;
    private String provider;
    private String model;
    private String errorMessage;
}
