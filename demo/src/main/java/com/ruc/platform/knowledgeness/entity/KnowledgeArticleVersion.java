package com.ruc.platform.knowledgeness.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_article_version")
public class KnowledgeArticleVersion {
    @TableId
    private Long id;
    private Long articleId;
    private Integer versionNo;
    private String snapshotJson;
    private Long createdBy;
    private LocalDateTime createdAt;
}
