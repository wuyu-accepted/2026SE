package com.ruc.platform.admin.audit.service;

import com.ruc.platform.admin.audit.dto.AuditLogQueryDTO;
import com.ruc.platform.admin.audit.vo.AuditLogVO;
import com.ruc.platform.common.api.PageResult;

public interface AuditLogService {
    PageResult<AuditLogVO> list(AuditLogQueryDTO query);
}
