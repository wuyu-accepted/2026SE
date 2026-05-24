package com.ruc.platform.ai.vo;

import lombok.Data;

@Data
public class AiActionVO {
    private String code;
    private String title;
    private String description;
    private String path;
    private Boolean tabPage;
    private Integer score;
}
