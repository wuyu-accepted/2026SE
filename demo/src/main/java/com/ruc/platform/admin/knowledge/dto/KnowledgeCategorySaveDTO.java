package com.ruc.platform.admin.knowledge.dto;

import lombok.Data;

@Data
public class KnowledgeCategorySaveDTO {

    private String name;
    private String code;
    private Integer sortOrder;
    private Integer status;
}
