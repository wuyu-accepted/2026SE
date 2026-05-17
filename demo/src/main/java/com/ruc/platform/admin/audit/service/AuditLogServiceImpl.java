package com.ruc.platform.admin.audit.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruc.platform.admin.audit.dto.AuditLogQueryDTO;
import com.ruc.platform.admin.audit.mapper.AuditLogMapper;
import com.ruc.platform.admin.audit.vo.AuditLogVO;
import com.ruc.platform.common.api.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;

    @Override
    public PageResult<AuditLogVO> list(AuditLogQueryDTO query) {
        IPage<?> page = new Page<>(query.getPageNum(), query.getPageSize());
        List<AuditLogVO> records = auditLogMapper.selectPage(page, query);
        Long total = auditLogMapper.count(query);
        return PageResult.of(total, query.getPageNum(), query.getPageSize(), records);
    }
}
