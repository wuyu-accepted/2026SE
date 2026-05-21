package com.ruc.platform.knowledgeness.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeRecommendationVO {

    private String targetType;

    private Long targetId;

    private String title;

    private String summary;

    private String categoryName;

    private String contentType;

    private String tags;

    private Long fileId;

    private String format;

    private Integer score;

    private String recommendReason;

    private LocalDateTime publishTime;
}
