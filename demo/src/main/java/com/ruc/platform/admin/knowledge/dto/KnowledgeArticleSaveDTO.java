package com.ruc.platform.admin.knowledge.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeArticleSaveDTO {

    private Long categoryId;
    private String title;
    private String summary;
    private String content;
    private String answer;
    private String source;
    private String contentType;
    private Long fileId;
    private String contentMode;
    private String editorType;
    private String sourceContent;
    private String tags;
    private String targetGrades;
    private String targetMajors;
    private String targetPoliticalStatuses;
    private String targetPartyStages;
    private String scenarioCodes;
    private Integer priority;
    private Integer status;
    private Boolean isBanner;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private String applicableScope;
    private String referenceArticleIds;
}
