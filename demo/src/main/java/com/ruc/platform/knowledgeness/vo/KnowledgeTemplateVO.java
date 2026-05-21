package com.ruc.platform.knowledgeness.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识模板VO
 */
@Data
public class KnowledgeTemplateVO {

    /**
     * 模板ID
     */
    private Long id;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 模板分类
     */
    private String category;

    private String tags;

    private String targetGrades;

    private String targetMajors;

    private String targetPoliticalStatuses;

    private String targetPartyStages;

    private String scenarioCodes;

    private Integer priority;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;

    /**
     * 关联文件ID
     */
    private Long fileId;

    /**
     * 文件格式
     */
    private String format;

    /**
     * 下载次数
     */
    private Long downloadCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
