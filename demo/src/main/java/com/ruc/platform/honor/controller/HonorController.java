package com.ruc.platform.honor.controller;

import com.ruc.platform.common.api.Result;
import com.ruc.platform.honor.entity.Honor;
import com.ruc.platform.honor.mapper.HonorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/honor")
@RequiredArgsConstructor
public class HonorController {

    private final HonorMapper honorMapper;

    @GetMapping
    public Result<List<Honor>> list() {
        return Result.ok(honorMapper.selectEnabledOrderByCreatedAtDesc());
    }
}
