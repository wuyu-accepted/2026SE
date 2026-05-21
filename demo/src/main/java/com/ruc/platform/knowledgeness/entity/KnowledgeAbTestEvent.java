package com.ruc.platform.knowledgeness.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_ab_test_event")
public class KnowledgeAbTestEvent {
    @TableId
    private Long id;
    private Long userId;
    private String abGroup;
    private String eventType;
    private String targetType;
    private Long targetId;
    private LocalDateTime createdAt;
}
