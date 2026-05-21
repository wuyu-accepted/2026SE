package com.ruc.platform.admin.knowledge.dto;

import lombok.Data;

@Data
public class KnowledgeSynonymSaveDTO {
    private String groupName;
    private String terms;
    private Integer status;
}
