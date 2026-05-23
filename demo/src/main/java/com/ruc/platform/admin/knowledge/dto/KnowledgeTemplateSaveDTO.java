package com.ruc.platform.admin.knowledge.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeTemplateSaveDTO {

    private String name;
    private String description;
    private String category;
    private Long fileId;
    private String format;
    private String tags;
    private String targetGrades;
    private String targetMajors;
    private String targetPoliticalStatuses;
    private String targetPartyStages;
    private String scenarioCodes;
    private Integer priority;
    private Integer status;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
}
