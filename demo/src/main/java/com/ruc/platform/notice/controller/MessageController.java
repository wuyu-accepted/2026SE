package com.ruc.platform.notice.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ruc.platform.common.api.Result;
import com.ruc.platform.notice.service.MessageService;
import com.ruc.platform.notice.vo.MessageDetailVO;
import com.ruc.platform.notice.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/recent")
    public Result<List<MessageVO>> getRecentMessages(
            @RequestParam(defaultValue = "10") Integer limit) {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(messageService.getRecentMessages(userId, limit));
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Long>> getUnreadCount() {
        long userId = StpUtil.getLoginIdAsLong();
        Long count = messageService.getUnreadCount(userId);
        Map<String, Long> result = new HashMap<>();
        result.put("unreadCount", count);
        return Result.ok(result);
    }

    @GetMapping("/{id}")
    public Result<MessageDetailVO> getMessageDetail(@PathVariable Long id) {
        long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(messageService.getMessageDetail(userId, id));
    }

    @PostMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        long userId = StpUtil.getLoginIdAsLong();
        messageService.markAsRead(userId, id);
        return Result.ok();
    }
}
