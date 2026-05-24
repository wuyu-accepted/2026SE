package com.ruc.platform.ai.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ruc.platform.ai.dto.AiChatRequest;
import com.ruc.platform.ai.service.AiChatService;
import com.ruc.platform.ai.vo.AiChatResponse;
import com.ruc.platform.auth.mapper.UserMapper;
import com.ruc.platform.common.api.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {
    private final AiChatService aiChatService;
    private final UserMapper userMapper;

    @PostMapping("/chat")
    public Result<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(aiChatService.chat(userId, new HashSet<>(userMapper.selectRoleCodesByUserId(userId)), request));
    }
}
