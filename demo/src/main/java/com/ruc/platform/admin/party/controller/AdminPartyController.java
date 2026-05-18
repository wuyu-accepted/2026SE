package com.ruc.platform.admin.party.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ruc.platform.admin.party.dto.*;
import com.ruc.platform.admin.party.service.AdminPartyService;
import com.ruc.platform.admin.party.vo.*;
import com.ruc.platform.common.api.PageResult;
import com.ruc.platform.common.api.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/party")
@RequiredArgsConstructor
public class AdminPartyController {

    private final AdminPartyService adminPartyService;

    @GetMapping("/stages")
    public Result<List<Map<String, Object>>> listStages() {
        return Result.ok(adminPartyService.listStages());
    }

    @GetMapping("/steps")
    public Result<List<Map<String, Object>>> listSteps(@RequestParam String stageCode) {
        return Result.ok(adminPartyService.listSteps(stageCode));
    }

    @GetMapping("/progress")
    public Result<PageResult<PartyStudentProgressVO>> listStudentProgress(PartyStudentProgressQueryDTO query) {
        return Result.ok(adminPartyService.listStudentProgress(query));
    }

    @PutMapping("/progress/{userId}")
    public Result<Void> updateStudentProgress(@PathVariable Long userId, @RequestBody UpdateProgressDTO dto) {
        adminPartyService.updateStudentProgress(userId, dto);
        return Result.ok();
    }

    @PostMapping("/progress/batch-import")
    public Result<Void> batchImportProgress(@RequestBody BatchImportProgressDTO dto) {
        adminPartyService.batchImportProgress(dto);
        return Result.ok();
    }

    @GetMapping("/reports")
    public Result<PageResult<PartyReportVO>> listReports(ReportReviewQueryDTO query) {
        return Result.ok(adminPartyService.listReports(query));
    }

    @GetMapping("/reports/{id}")
    public Result<PartyReportVO> getReportDetail(@PathVariable Long id) {
        return Result.ok(adminPartyService.getReportDetail(id));
    }

    @PostMapping("/reports/{id}/approve")
    public Result<Void> approveReport(@PathVariable Long id, @RequestBody ReportReviewDTO dto) {
        long reviewerId = StpUtil.getLoginIdAsLong();
        adminPartyService.approveReport(id, reviewerId, dto);
        return Result.ok();
    }

    @PostMapping("/reports/{id}/reject")
    public Result<Void> rejectReport(@PathVariable Long id, @RequestBody ReportReviewDTO dto) {
        long reviewerId = StpUtil.getLoginIdAsLong();
        adminPartyService.rejectReport(id, reviewerId, dto);
        return Result.ok();
    }

    @GetMapping("/activities")
    public Result<PageResult<PartyActivityVO>> listActivities(ActivityReviewQueryDTO query) {
        return Result.ok(adminPartyService.listActivities(query));
    }

    @GetMapping("/activities/{id}")
    public Result<PartyActivityVO> getActivityDetail(@PathVariable Long id) {
        return Result.ok(adminPartyService.getActivityDetail(id));
    }

    @PostMapping("/activities/{id}/approve")
    public Result<Void> approveActivity(@PathVariable Long id, @RequestBody ActivityReviewDTO dto) {
        long reviewerId = StpUtil.getLoginIdAsLong();
        adminPartyService.approveActivity(id, reviewerId, dto);
        return Result.ok();
    }

    @PostMapping("/activities/{id}/reject")
    public Result<Void> rejectActivity(@PathVariable Long id, @RequestBody ActivityReviewDTO dto) {
        long reviewerId = StpUtil.getLoginIdAsLong();
        adminPartyService.rejectActivity(id, reviewerId, dto);
        return Result.ok();
    }
}
