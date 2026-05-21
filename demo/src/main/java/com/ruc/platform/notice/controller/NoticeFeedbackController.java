package com.ruc.platform.notice.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ruc.platform.common.api.PageResult;
import com.ruc.platform.common.api.Result;
import com.ruc.platform.notice.dto.NoticeFeedbackReplyDTO;
import com.ruc.platform.notice.service.NoticeFeedbackService;
import com.ruc.platform.notice.vo.NoticeFeedbackDetailVO;
import com.ruc.platform.notice.vo.NoticeFeedbackVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notice-feedback")
@RequiredArgsConstructor
public class NoticeFeedbackController {

    private final NoticeFeedbackService noticeFeedbackService;

    @GetMapping("/cadre/pending")
    public Result<PageResult<NoticeFeedbackVO>> listCadrePending(@RequestParam(defaultValue = "1") Long pageNum,
                                                                 @RequestParam(defaultValue = "10") Long pageSize) {
        return Result.ok(noticeFeedbackService.listCadrePending(StpUtil.getLoginIdAsLong(), pageNum, pageSize));
    }

    @GetMapping("/cadre/{id}")
    public Result<NoticeFeedbackDetailVO> getCadreDetail(@PathVariable Long id) {
        return Result.ok(noticeFeedbackService.getCadreDetail(StpUtil.getLoginIdAsLong(), id));
    }

    @PostMapping("/{id}/cadre-reply")
    public Result<Void> cadreReply(@PathVariable Long id, @Valid @RequestBody NoticeFeedbackReplyDTO dto) {
        noticeFeedbackService.cadreReply(StpUtil.getLoginIdAsLong(), id, dto);
        return Result.ok();
    }

    @PostMapping("/{id}/escalate")
    public Result<Void> escalate(@PathVariable Long id, @Valid @RequestBody NoticeFeedbackReplyDTO dto) {
        noticeFeedbackService.escalateToCounselor(StpUtil.getLoginIdAsLong(), id, dto);
        return Result.ok();
    }

    @GetMapping("/counselor/pending")
    public Result<PageResult<NoticeFeedbackVO>> listCounselorPending(@RequestParam(defaultValue = "1") Long pageNum,
                                                                     @RequestParam(defaultValue = "10") Long pageSize,
                                                                     @RequestParam(required = false) String feedbackType,
                                                                     @RequestParam(required = false) String status,
                                                                     @RequestParam(required = false) Long noticeId) {
        return Result.ok(noticeFeedbackService.listCounselorPending(StpUtil.getLoginIdAsLong(), pageNum, pageSize, feedbackType, status, noticeId));
    }

    @GetMapping("/counselor/pending-count")
    public Result<Long> countCounselorPending() {
        return Result.ok(noticeFeedbackService.countCounselorPending(StpUtil.getLoginIdAsLong()));
    }

    @GetMapping("/counselor/{id}")
    public Result<NoticeFeedbackDetailVO> getCounselorDetail(@PathVariable Long id) {
        return Result.ok(noticeFeedbackService.getCounselorDetail(StpUtil.getLoginIdAsLong(), id));
    }

    @PostMapping("/{id}/counselor-reply")
    public Result<Void> counselorReply(@PathVariable Long id, @Valid @RequestBody NoticeFeedbackReplyDTO dto) {
        noticeFeedbackService.counselorReply(StpUtil.getLoginIdAsLong(), id, dto);
        return Result.ok();
    }
}
