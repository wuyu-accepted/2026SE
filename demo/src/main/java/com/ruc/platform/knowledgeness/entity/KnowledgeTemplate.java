package com.ruc.platform.knowledgeness.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识模板实体类
 * 对应数据库表：knowledge_template
 */
@Data
@TableName("knowledge_template")
public class KnowledgeTemplate {

    /**
     * 模板ID（主键）
     */
    @TableId
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
     * 文件格式：DOCX/XLSX/PDF
     */
    private String format;

    /**
     * 下载次数
     */
    private Long downloadCount;

    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;

    private Long createdBy;

    private Long updatedBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
