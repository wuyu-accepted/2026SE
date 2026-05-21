package com.ruc.platform.knowledgeness.dto;

import lombok.Data;

@Data
public class KnowledgeBehaviorDTO {

    private String eventType;

    private String targetType;

    private Long targetId;

    private String keyword;

    private String sourcePage;
}
