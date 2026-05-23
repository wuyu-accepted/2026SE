package com.ruc.platform.knowledgeness.dto;

import lombok.Data;

@Data
public class KnowledgeTemplateQueryDTO {

    private String keyword;

    private String category;

    private String tag;

    private String scenarioCode;

    private Integer status = 1;
}
