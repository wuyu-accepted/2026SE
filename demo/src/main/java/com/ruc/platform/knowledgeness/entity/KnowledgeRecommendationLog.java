package com.ruc.platform.knowledgeness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_recommendation_log")
public class KnowledgeRecommendationLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String targetType;

    private Long targetId;

    private Integer score;

    private String reason;

    private String strategyVersion;

    private String featureSnapshot;

    private LocalDateTime createdAt;
}
