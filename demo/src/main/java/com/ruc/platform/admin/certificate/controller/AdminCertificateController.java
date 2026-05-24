package com.ruc.platform.admin.certificate.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ruc.platform.certificate.entity.ECertificate;
import com.ruc.platform.certificate.mapper.ECertificateMapper;
import com.ruc.platform.common.api.Result;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/certificates")
@RequiredArgsConstructor
public class AdminCertificateController {

    private final ECertificateMapper certificateMapper;

    @GetMapping
    public Result<List<ECertificate>> listAll(@RequestParam(required = false) Integer status) {
        List<ECertificate> list;
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ECertificate> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ECertificate>();
        if (status != null) {
            wrapper.eq(ECertificate::getStatus, status);
        }
        wrapper.orderByDesc(ECertificate::getSubmitTime);
        list = certificateMapper.selectList(wrapper);
        return Result.ok(list);
    }

    @GetMapping("/{id}")
    public Result<ECertificate> getDetail(@PathVariable Long id) {
        ECertificate cert = certificateMapper.selectById(id);
        if (cert == null) {
            return Result.fail(404, "申请不存在");
        }
        return Result.ok(cert);
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        Long adminId = StpUtil.getLoginIdAsLong();
        ECertificate cert = certificateMapper.selectById(id);
        if (cert == null) {
            return Result.fail(404, "申请不存在");
        }
        cert.setStatus(2);
        cert.setApprovedBy(adminId);
        cert.setApprovedAt(LocalDateTime.now());
        cert.setUpdatedAt(LocalDateTime.now());
        certificateMapper.updateById(cert);
        log.info("审批通过电子证明申请，id: {}, adminId: {}", id, adminId);
        return Result.ok();
    }

    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody(required = false) RejectDTO rejectDTO) {
        ECertificate cert = certificateMapper.selectById(id);
        if (cert == null) {
            return Result.fail(404, "申请不存在");
        }
        cert.setStatus(3);
        cert.setRejectReason(rejectDTO != null ? rejectDTO.getRejectReason() : null);
        cert.setUpdatedAt(LocalDateTime.now());
        certificateMapper.updateById(cert);
        log.info("驳回电子证明申请，id: {}, reason: {}", id, cert.getRejectReason());
        return Result.ok();
    }

    @Data
    public static class RejectDTO {
        private String rejectReason;
    }
}
