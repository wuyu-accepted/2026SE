package com.ruc.platform.admin.certificate.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ruc.platform.certificate.entity.ECertificate;
import com.ruc.platform.certificate.mapper.ECertificateMapper;
import com.ruc.platform.common.api.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
        if (status != null) {
            list = certificateMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ECertificate>()
                            .eq(ECertificate::getStatus, status)
                            .orderByDesc(ECertificate::getSubmitTime));
        } else {
            list = certificateMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ECertificate>()
                            .orderByDesc(ECertificate::getSubmitTime));
        }
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
    public Result<Void> approve(@PathVariable Long id, @Valid @RequestBody ReviewDTO reviewDTO) {
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
    public Result<Void> reject(@PathVariable Long id, @Valid @RequestBody ReviewDTO reviewDTO) {
        Long adminId = StpUtil.getLoginIdAsLong();
        ECertificate cert = certificateMapper.selectById(id);
        if (cert == null) {
            return Result.fail(404, "申请不存在");
        }
        cert.setStatus(3);
        cert.setRejectReason(reviewDTO.getReason());
        cert.setUpdatedAt(LocalDateTime.now());
        certificateMapper.updateById(cert);
        log.info("驳回电子证明申请，id: {}, adminId: {}, reason: {}", id, adminId, reviewDTO.getReason());
        return Result.ok();
    }

    @Data
    public static class ReviewDTO {
        @NotBlank(message = "审批意见不能为空")
        private String reason;
    }
}
