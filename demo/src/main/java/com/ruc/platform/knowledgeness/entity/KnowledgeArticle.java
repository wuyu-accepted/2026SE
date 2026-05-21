package com.ruc.platform.knowledgeness.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识条目实体类
 * 对应数据库表：knowledge_article
 */
@Data
@TableName("knowledge_article")
public class KnowledgeArticle {

    /**
     * 条目ID（主键）
     */
    @TableId
    private Long id;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 标题
     */
    private String title;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 详细内容
     */
    private String content;

    /**
     * 标准答案/办理建议
     */
    private String answer;

    /**
     * 来源：制度摘要/辅导说明/办理指引
     */
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

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;

    private String extractedText;

    private LocalDateTime extractedAt;

    private String extractStatus;

    private String extractError;

    /**
     * 状态：0-草稿，1-已发布，2-已下架
     */
    private Integer status;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 浏览次数
     */
    private Long viewCount;

    /**
     * 创建人ID
     */
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
