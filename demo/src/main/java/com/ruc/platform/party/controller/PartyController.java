package com.ruc.platform.party.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ruc.platform.common.api.Result;
import com.ruc.platform.party.dto.PartyReportDTO;
import com.ruc.platform.party.dto.PartyActivityCreateDTO;
import com.ruc.platform.party.entity.PartyReport;
import com.ruc.platform.party.service.PartyService;
import com.ruc.platform.party.vo.PartyActivityListItemVO;
import com.ruc.platform.party.vo.PartyOverviewVO;
import com.ruc.platform.party.vo.PartyRecordVO;
import com.ruc.platform.party.vo.PartyReportListItemVO;
import com.ruc.platform.party.vo.PartyReminderVO;
import com.ruc.platform.party.vo.PartyTrackerVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 党团控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/party")
@RequiredArgsConstructor
public class PartyController {

    private final PartyService partyService;

    @GetMapping("/me/overview")
    public Result<PartyOverviewVO> getOverview() {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(partyService.getOverview(userId));
    }

    @GetMapping("/me/progress")
    public Result<PartyTrackerVO> getProgress() {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(partyService.getTracker(userId));
    }

    @GetMapping("/me/records")
    public Result<List<PartyRecordVO>> getRecords() {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(partyService.getRecords(userId));
    }

    @GetMapping("/me/reminders")
    public Result<List<PartyReminderVO>> getReminders() {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(partyService.getReminders(userId));
    }

    @GetMapping("/me/tracker")
    public Result<PartyTrackerVO> getTracker() {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(partyService.getTracker(userId));
    }

    @PostMapping("/me/reports")
    public Result<Void> submitReport(@Valid @RequestBody PartyReportDTO reportDTO) {
        long userId = StpUtil.getLoginIdAsLong();
        partyService.submitReport(userId, reportDTO);
        return Result.ok();
    }

    @GetMapping("/me/reports")
    public Result<List<PartyReportListItemVO>> listMyReports() {
        long userId = StpUtil.getLoginIdAsLong();
        List<PartyReport> list = partyService.listMyReports(userId);
        List<PartyReportListItemVO> vos = list.stream().map(item -> {
            PartyReportListItemVO vo = new PartyReportListItemVO();
            vo.setId(item.getId());
            vo.setTitle(item.getTitle());
            vo.setFileId(item.getFileId());
            vo.setStatus(item.getStatus());
            vo.setReviewComment(item.getReviewComment());
            vo.setSubmitTime(item.getSubmitTime());
            vo.setReviewedAt(item.getReviewedAt());
            return vo;
        }).collect(Collectors.toList());
        return Result.ok(vos);
    }

    @PostMapping("/me/activities")
    public Result<Map<String, Long>> createActivity(@Valid @RequestBody PartyActivityCreateDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        Long id = partyService.createActivityApplication(userId, dto);
        return Result.ok(Map.of("id", id));
    }

    @GetMapping("/me/activities")
    public Result<List<PartyActivityListItemVO>> listMyActivities() {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(partyService.listMyActivities(userId));
    }
}
