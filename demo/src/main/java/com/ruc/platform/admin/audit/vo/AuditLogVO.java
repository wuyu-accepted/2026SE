package com.ruc.platform.admin.audit.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLogVO {
    private Long id;
    private Long userId;
    private String userName;
    private String module;
    private String action;
    private String description;
    private String ipAddress;
    private Long executionTime;
    private Integer status;
    private String errorMessage;
    private LocalDateTime createdAt;
}
