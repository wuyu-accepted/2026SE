package com.ruc.platform.admin.party.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruc.platform.admin.party.dto.*;
import com.ruc.platform.admin.party.service.PartyAdminService;
import com.ruc.platform.admin.party.service.AdminPartyService;
import com.ruc.platform.admin.party.vo.*;
import com.ruc.platform.common.api.PageResult;
import com.ruc.platform.common.api.Result;
import com.ruc.platform.common.api.ResultCode;
import com.ruc.platform.common.exception.BizException;
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
    private final PartyAdminService partyAdminService;
    private final ObjectMapper objectMapper;

    @GetMapping("/stages")
    public Result<List<PartyStageOptionVO>> listStages() {
        return Result.ok(partyAdminService.listStageOptions());
    }

    @GetMapping("/steps")
    public Result<List<PartyStepOptionVO>> listSteps(@RequestParam String stageCode) {
        return Result.ok(partyAdminService.listStepOptions(stageCode));
    }

    @GetMapping("/progress")
    public Result<PageResult<PartyStudentProgressVO>> listStudentProgress(PartyStudentProgressQueryDTO query) {
        return Result.ok(adminPartyService.listStudentProgress(query));
    }

    @GetMapping("/progress/student")
    public Result<PartyStudentProgressAdminVO> getStudentProgressDetail(@RequestParam(required = false) String studentNo,
                                                                        @RequestParam(required = false) String realName) {
        return Result.ok(adminPartyService.getStudentProgressDetail(studentNo, realName));
    }

    @PutMapping("/progress/{userId}")
    public Result<Void> updateStudentProgress(@PathVariable Long userId, @RequestBody UpdateProgressDTO dto) {
        adminPartyService.updateStudentProgress(userId, dto);
        return Result.ok();
    }

    @PostMapping("/progress/batch-import")
    public Result<PartyProgressBatchImportResultVO> batchImportProgress(@RequestBody JsonNode body) {
        PartyProgressBatchImportDTO dto = parseBatchImportBody(body);
        return Result.ok(partyAdminService.batchImportProgress(dto));
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

    private PartyProgressBatchImportDTO parseBatchImportBody(JsonNode body) {
        if (body == null || body.isNull()) {
            throw new BizException(ResultCode.PARAM_ERROR, "导入数据不能为空");
        }
        JsonNode itemsNode;
        if (body.isArray()) {
            itemsNode = body;
        } else if (body.isObject()) {
            itemsNode = body.get("items");
        } else {
            throw new BizException(ResultCode.PARAM_ERROR, "导入数据格式不正确");
        }
        if (itemsNode == null || !itemsNode.isArray() || itemsNode.size() == 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "导入数据不能为空");
        }
        List<PartyProgressImportItemDTO> items = objectMapper.convertValue(itemsNode, new TypeReference<>() {});
        PartyProgressBatchImportDTO dto = new PartyProgressBatchImportDTO();
        dto.setItems(items);
        return dto;
    }
}
