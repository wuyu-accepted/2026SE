package com.ruc.platform.knowledgeness.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_quality_feedback")
public class KnowledgeQualityFeedback {
    @TableId
    private Long id;
    private Long articleId;
    private Long userId;
    private String feedbackType;
    private Integer score;
    private String comment;
    private LocalDateTime createdAt;
}
