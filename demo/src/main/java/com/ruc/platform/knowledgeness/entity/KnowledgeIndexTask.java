package com.ruc.platform.knowledgeness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_index_task")
public class KnowledgeIndexTask {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long articleId;

    private String status;

    private String triggerType;

    private Integer retryCount;

    private LocalDateTime nextRetryAt;

    private String lastError;

    private String taskLog;

    private Boolean ocrUsed;

    private Long durationMs;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime finishedAt;
}
