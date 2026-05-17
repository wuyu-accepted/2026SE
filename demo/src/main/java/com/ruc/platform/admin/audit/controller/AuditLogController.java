package com.ruc.platform.admin.audit.controller;

import com.ruc.platform.admin.audit.dto.AuditLogQueryDTO;
import com.ruc.platform.admin.audit.service.AuditLogService;
import com.ruc.platform.admin.audit.vo.AuditLogVO;
import com.ruc.platform.common.api.PageResult;
import com.ruc.platform.common.api.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public Result<PageResult<AuditLogVO>> list(AuditLogQueryDTO query) {
        return Result.ok(auditLogService.list(query));
    }
}
