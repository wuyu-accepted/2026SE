package com.ruc.platform.certificate.controller;

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
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/certificate")
@RequiredArgsConstructor
public class CertificateController {

    private final ECertificateMapper certificateMapper;

    @PostMapping("/me/applications")
    public Result<Map<String, Long>> create(@Valid @RequestBody CertificateCreateDTO createDTO) {
        Long userId = StpUtil.getLoginIdAsLong();
        ECertificate cert = new ECertificate();
        cert.setUserId(userId);
        cert.setTitle(createDTO.getTitle());
        cert.setReason(createDTO.getReason());
        cert.setTemplateType(createDTO.getTemplateType() != null ? createDTO.getTemplateType() : "general");
        cert.setStatus(0);
        cert.setSubmitTime(LocalDateTime.now());
        cert.setCreatedAt(LocalDateTime.now());
        cert.setUpdatedAt(LocalDateTime.now());
        certificateMapper.insert(cert);
        log.info("创建电子证明申请，userId: {}, id: {}", userId, cert.getId());
        return Result.ok(Map.of("id", cert.getId()));
    }

    @GetMapping("/me/applications")
    public Result<List<ECertificate>> listMine() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(certificateMapper.selectByUserId(userId));
    }

    @GetMapping("/me/applications/{id}")
    public Result<ECertificate> getMineDetail(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        ECertificate cert = certificateMapper.selectById(id);
        if (cert != null && !cert.getUserId().equals(userId)) {
            return Result.fail(403, "无权查看该申请");
        }
        return Result.ok(cert);
    }

    @Data
    public static class CertificateCreateDTO {
        @NotBlank(message = "证明标题不能为空")
        private String title;

        @NotBlank(message = "申请理由不能为空")
        private String reason;

        private String templateType;
    }
}
