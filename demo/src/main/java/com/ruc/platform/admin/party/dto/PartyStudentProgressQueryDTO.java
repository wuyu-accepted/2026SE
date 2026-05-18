package com.ruc.platform.admin.party.dto;

import lombok.Data;

@Data
public class PartyStudentProgressQueryDTO {
    private String stageCode;
    private String keyword;
    private Long pageNum = 1L;
    private Long pageSize = 20L;
}
