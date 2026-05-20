package com.ruc.platform.wechat.officialaccount.controller;

import com.ruc.platform.common.api.Result;
import com.ruc.platform.wechat.officialaccount.service.OfficialAccountMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/wechat/official-account/menu")
@RequiredArgsConstructor
public class OfficialAccountMenuController {

    private final OfficialAccountMenuService menuService;

    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh() {
        return Result.ok(menuService.refreshDefaultMenu());
    }
}
