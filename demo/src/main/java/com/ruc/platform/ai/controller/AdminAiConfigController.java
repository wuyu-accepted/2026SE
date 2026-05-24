package com.ruc.platform.ai.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ruc.platform.ai.dto.AiConfigSaveDTO;
import com.ruc.platform.ai.dto.AiConfigTestDTO;
import com.ruc.platform.ai.service.AiConfigService;
import com.ruc.platform.ai.vo.AiConfigTestVO;
import com.ruc.platform.ai.vo.AiConfigVO;
import com.ruc.platform.common.api.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ai-config")
@RequiredArgsConstructor
public class AdminAiConfigController {
    private final AiConfigService aiConfigService;

    @GetMapping
    public Result<List<AiConfigVO>> list() {
        return Result.ok(aiConfigService.list());
    }

    @GetMapping("/active")
    public Result<AiConfigVO> active() {
        return Result.ok(aiConfigService.active());
    }

    @PostMapping
    public Result<Long> create(@RequestBody AiConfigSaveDTO dto) {
        return Result.ok(aiConfigService.create(StpUtil.getLoginIdAsLong(), dto));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody AiConfigSaveDTO dto) {
        aiConfigService.update(StpUtil.getLoginIdAsLong(), id, dto);
        return Result.ok();
    }

    @PostMapping("/{id}/activate")
    public Result<Void> activate(@PathVariable Long id) {
        aiConfigService.activate(StpUtil.getLoginIdAsLong(), id);
        return Result.ok();
    }

    @PostMapping("/{id}/test")
    public Result<AiConfigTestVO> test(@PathVariable Long id, @RequestBody(required = false) AiConfigTestDTO dto) {
        return Result.ok(aiConfigService.test(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        aiConfigService.delete(id);
        return Result.ok();
    }
}
