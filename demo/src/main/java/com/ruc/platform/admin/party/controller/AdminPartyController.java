package com.ruc.platform.admin.party.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ruc.platform.admin.party.dto.PartyProgressBatchImportDTO;
import com.ruc.platform.admin.party.dto.PartyReviewDTO;
import com.ruc.platform.admin.party.service.AdminPartyService;
import com.ruc.platform.admin.party.vo.*;
import com.ruc.platform.auth.mapper.UserMapper;
import com.ruc.platform.common.api.Result;
import com.ruc.platform.common.api.ResultCode;
import com.ruc.platform.common.exception.BizException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/party")
@RequiredArgsConstructor
public class AdminPartyController {

    private final AdminPartyService adminPartyService;
    private final UserMapper userMapper;

    @GetMapping("/stages")
    public Result<List<PartyStageOptionVO>> listStages() {
        requireCounselorOrAdmin();
        return Result.ok(adminPartyService.listStageOptions());
    }

    @GetMapping("/steps")
    public Result<List<PartyStepOptionVO>> listSteps(@RequestParam String stageCode) {
        requireCounselorOrAdmin();
        return Result.ok(adminPartyService.listStepOptions(stageCode));
    }

    @PostMapping("/progress/batch-import")
    public Result<PartyProgressBatchImportResultVO> batchImport(@Valid @RequestBody PartyProgressBatchImportDTO dto) {
        long operatorUserId = StpUtil.getLoginIdAsLong();
        requireCounselorOrAdmin(operatorUserId);
        return Result.ok(adminPartyService.batchImportProgress(operatorUserId, dto));
    }

    @GetMapping("/progress/student")
    public Result<PartyStudentProgressAdminVO> queryStudentProgress(@RequestParam(required = false) String studentNo,
                                                                    @RequestParam(required = false) String realName) {
        requireCounselorOrAdmin();
        return Result.ok(adminPartyService.queryStudentProgress(studentNo, realName));
    }

    @GetMapping("/reports")
    public Result<List<PartyReportAdminListItemVO>> listReports(@RequestParam(required = false) Integer status) {
        requireCounselorOrAdmin();
        return Result.ok(adminPartyService.listReports(status));
    }

    @GetMapping("/reports/{id}")
    public Result<PartyReportAdminDetailVO> getReport(@PathVariable Long id) {
        requireCounselorOrAdmin();
        return Result.ok(adminPartyService.getReportDetail(id));
    }

    @PostMapping("/reports/{id}/approve")
    public Result<Void> approveReport(@PathVariable Long id, @Valid @RequestBody PartyReviewDTO dto) {
        long operatorUserId = StpUtil.getLoginIdAsLong();
        requireCounselorOrAdmin(operatorUserId);
        adminPartyService.approveReport(operatorUserId, id, dto.getComment());
        return Result.ok();
    }

    @PostMapping("/reports/{id}/reject")
    public Result<Void> rejectReport(@PathVariable Long id, @Valid @RequestBody PartyReviewDTO dto) {
        long operatorUserId = StpUtil.getLoginIdAsLong();
        requireCounselorOrAdmin(operatorUserId);
        adminPartyService.rejectReport(operatorUserId, id, dto.getComment());
        return Result.ok();
    }

    @GetMapping("/activities")
    public Result<List<PartyActivityAdminListItemVO>> listActivities(@RequestParam(required = false) Integer status) {
        requireCounselorOrAdmin();
        return Result.ok(adminPartyService.listActivities(status));
    }

    @GetMapping("/activities/{id}")
    public Result<PartyActivityAdminDetailVO> getActivity(@PathVariable Long id) {
        requireCounselorOrAdmin();
        return Result.ok(adminPartyService.getActivityDetail(id));
    }

    @PostMapping("/activities/{id}/approve")
    public Result<Void> approveActivity(@PathVariable Long id, @Valid @RequestBody PartyReviewDTO dto) {
        long operatorUserId = StpUtil.getLoginIdAsLong();
        requireCounselorOrAdmin(operatorUserId);
        adminPartyService.approveActivity(operatorUserId, id, dto.getComment());
        return Result.ok();
    }

    @PostMapping("/activities/{id}/reject")
    public Result<Void> rejectActivity(@PathVariable Long id, @Valid @RequestBody PartyReviewDTO dto) {
        long operatorUserId = StpUtil.getLoginIdAsLong();
        requireCounselorOrAdmin(operatorUserId);
        adminPartyService.rejectActivity(operatorUserId, id, dto.getComment());
        return Result.ok();
    }

    private void requireCounselorOrAdmin() {
        long operatorUserId = StpUtil.getLoginIdAsLong();
        requireCounselorOrAdmin(operatorUserId);
    }

    private void requireCounselorOrAdmin(long operatorUserId) {
        List<String> roles = userMapper.selectRoleCodesByUserId(operatorUserId);
        if (roles == null || (!roles.contains("admin") && !roles.contains("counselor"))) {
            throw new BizException(ResultCode.FORBIDDEN, "无权限");
        }
    }
}
