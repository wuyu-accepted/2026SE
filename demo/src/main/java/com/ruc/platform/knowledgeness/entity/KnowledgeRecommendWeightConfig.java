package com.ruc.platform.knowledgeness.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_recommend_weight_config")
public class KnowledgeRecommendWeightConfig {
    @TableId
    private Long id;
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
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
