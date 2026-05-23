package com.ruc.platform.knowledgeness.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识条目列表项VO
 */
@Data
public class KnowledgeArticleListItemVO {

    /**
     * 条目ID
     */
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 分类名称
     */
    private String categoryName;

    private String contentType;

    private Long fileId;

    private String contentMode;

    private String editorType;

    private String tags;

    private String targetGrades;

    private String targetMajors;

    private String targetPoliticalStatuses;

    private String targetPartyStages;

    private String scenarioCodes;

    private Integer priority;

    private Integer status;

    private String extractStatus;

    private String extractError;

    private String ocrStatus;

    private String ocrError;

    private java.math.BigDecimal qualityScore;

    private String searchHighlight;

    private String scoreExplanation;

    private String correctedKeyword;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 浏览次数
     */
    private Long viewCount;
}
