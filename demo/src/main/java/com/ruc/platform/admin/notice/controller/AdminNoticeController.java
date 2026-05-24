package com.ruc.platform.admin.notice.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ruc.platform.admin.notice.dto.NoticeCreateDTO;
import com.ruc.platform.admin.notice.dto.NoticeQueryDTO;
import com.ruc.platform.admin.notice.dto.NoticeUpdateDTO;
import com.ruc.platform.admin.notice.service.AdminNoticeService;
import com.ruc.platform.admin.notice.vo.NoticeDetailVO;
import com.ruc.platform.admin.notice.vo.NoticeListItemVO;
import com.ruc.platform.admin.notice.vo.NoticePublishResultVO;
import com.ruc.platform.admin.notice.vo.NoticeStatsVO;
import com.ruc.platform.admin.notice.vo.NoticeTargetEstimateVO;
import com.ruc.platform.common.api.PageResult;
import com.ruc.platform.common.api.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final AdminNoticeService adminNoticeService;

    @GetMapping
    public Result<PageResult<NoticeListItemVO>> listNotices(NoticeQueryDTO queryDTO) {
        return Result.ok(adminNoticeService.listNotices(queryDTO));
    }

    @GetMapping("/{id}")
    public Result<NoticeDetailVO> getNoticeDetail(@PathVariable Long id) {
        return Result.ok(adminNoticeService.getNoticeDetail(id));
    }

    @PostMapping
    public Result<NoticeDetailVO> createNotice(@Valid @RequestBody NoticeCreateDTO createDTO) {
        Long creatorId = StpUtil.getLoginIdAsLong();
        log.info("创建通知草稿，creatorId: {}, title: {}", creatorId, createDTO.getTitle());
        return Result.ok(adminNoticeService.createNotice(creatorId, createDTO));
    }

    @PutMapping("/{id}")
    public Result<NoticeDetailVO> updateNotice(@PathVariable Long id, @Valid @RequestBody NoticeUpdateDTO updateDTO) {
        return Result.ok(adminNoticeService.updateNotice(id, updateDTO));
    }

    @PostMapping("/{id}/publish")
    public Result<NoticePublishResultVO> publishNotice(@PathVariable Long id) {
        return Result.ok(adminNoticeService.publishNotice(id, StpUtil.getLoginIdAsLong()));
    }

    @GetMapping("/{id}/target-estimate")
    public Result<NoticeTargetEstimateVO> estimateTarget(@PathVariable Long id) {
        return Result.ok(adminNoticeService.estimateTarget(id));
    }

    @GetMapping("/{id}/stats")
    public Result<NoticeStatsVO> getNoticeStats(@PathVariable Long id) {
        return Result.ok(adminNoticeService.getNoticeStats(id));
    }

    @PostMapping("/{id}/offline")
    public Result<Void> offlineNotice(@PathVariable Long id) {
        adminNoticeService.offlineNotice(id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteNotice(@PathVariable Long id) {
        adminNoticeService.deleteNotice(id);
        return Result.ok();
    }
}
