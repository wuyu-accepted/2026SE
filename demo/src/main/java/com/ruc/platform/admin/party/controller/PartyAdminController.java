package com.ruc.platform.admin.party.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ruc.platform.admin.party.dto.PartyProgressBatchImportDTO;
import com.ruc.platform.admin.party.service.PartyAdminService;
import com.ruc.platform.admin.party.vo.PartyProgressBatchImportResultVO;
import com.ruc.platform.admin.party.vo.PartyStageOptionVO;
import com.ruc.platform.admin.party.vo.PartyStepOptionVO;
import com.ruc.platform.auth.mapper.UserMapper;
import com.ruc.platform.common.api.Result;
import com.ruc.platform.common.api.ResultCode;
import com.ruc.platform.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/party")
@RequiredArgsConstructor
public class PartyAdminController {

    private final PartyAdminService partyAdminService;
    private final UserMapper userMapper;

    @GetMapping("/stages")
    public Result<List<PartyStageOptionVO>> listStages() {
        requireCounselorOrAdmin();
        return Result.ok(partyAdminService.listStageOptions());
    }

    @GetMapping("/steps")
    public Result<List<PartyStepOptionVO>> listSteps(@RequestParam String stageCode) {
        requireCounselorOrAdmin();
        return Result.ok(partyAdminService.listStepOptions(stageCode));
    }

    @PostMapping("/progress/batch-import")
    public Result<PartyProgressBatchImportResultVO> batchImport(@RequestBody PartyProgressBatchImportDTO dto) {
        requireCounselorOrAdmin();
        return Result.ok(partyAdminService.batchImportProgress(dto));
    }

    private void requireCounselorOrAdmin() {
        long userId = StpUtil.getLoginIdAsLong();
        List<String> roles = userMapper.selectRoleCodesByUserId(userId);
        if (roles == null || (!roles.contains("counselor") && !roles.contains("admin"))) {
            throw new BizException(ResultCode.FORBIDDEN, "无权限访问该接口");
        }
    }
}
