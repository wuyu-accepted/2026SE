package com.ruc.platform.party.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ruc.platform.common.api.Result;
import com.ruc.platform.party.dto.PartyReportDTO;
import com.ruc.platform.party.service.PartyService;
import com.ruc.platform.party.vo.PartyOverviewVO;
import com.ruc.platform.party.vo.PartyRecordVO;
import com.ruc.platform.party.vo.PartyReminderVO;
import com.ruc.platform.party.vo.PartyTrackerVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
