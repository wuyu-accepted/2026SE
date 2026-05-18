package com.ruc.platform.admin.party.dto;

import lombok.Data;

@Data
public class ActivityReviewQueryDTO {
    private Integer status;
    private String keyword;
    private Long pageNum = 1L;
    private Long pageSize = 20L;
}
