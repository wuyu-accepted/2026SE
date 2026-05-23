package com.ruc.platform.knowledgeness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_behavior_event")
public class KnowledgeBehaviorEvent {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String eventType;

    private String targetType;

    private Long targetId;

    private String keyword;

    private String sourcePage;

    private String featureSnapshot;

    private LocalDateTime createdAt;
}
