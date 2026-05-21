package com.ruc.platform.admin.knowledge.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class KnowledgeRecommendWeightConfigDTO {
    private String configName;
    private String abGroup;
    private BigDecimal profileWeight;
    private BigDecimal scenarioWeight;
    private BigDecimal behaviorWeight;
    private BigDecimal favoriteWeight;
    private BigDecimal downloadWeight;
    private BigDecimal successWeight;
    private BigDecimal similarStudentWeight;
    private BigDecimal timeDecayWeight;
    private Boolean enabled;
}
