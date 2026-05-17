package com.ruc.platform.admin.audit.dto;

import lombok.Data;

@Data
public class AuditLogQueryDTO {
    private String module;
    private String action;
    private Integer status;
    private String keyword;
    private Long pageNum = 1L;
    private Long pageSize = 20L;
}
