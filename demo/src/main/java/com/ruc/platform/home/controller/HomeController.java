package com.ruc.platform.home.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ruc.platform.common.api.Result;
import com.ruc.platform.home.service.HomeService;
import com.ruc.platform.home.vo.HomeVO;
import com.ruc.platform.home.entity.UserQuickEntry;
import com.ruc.platform.home.mapper.UserQuickEntryMapper;
import com.ruc.platform.home.entity.HomeBanner;
import com.ruc.platform.home.mapper.HomeBannerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;
    private final UserQuickEntryMapper userQuickEntryMapper;
    private final HomeBannerMapper homeBannerMapper;

    @GetMapping
    public Result<HomeVO> getHomeData() {
        long userId = StpUtil.getLoginIdAsLong();
        log.info("获取首页数据，userId: {}", userId);
        return Result.ok(homeService.getHomeData(userId));
    }

    @GetMapping("/quick-entries")
    public Result<List<UserQuickEntry>> getQuickEntries() {
        long userId = StpUtil.getLoginIdAsLong();
        List<UserQuickEntry> entries = userQuickEntryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserQuickEntry>()
                        .eq(UserQuickEntry::getUserId, userId)
                        .orderByAsc(UserQuickEntry::getSortOrder)
        );
        return Result.ok(entries);
    }

    @PostMapping("/quick-entries")
    public Result<Void> saveQuickEntries(@RequestBody List<Map<String, Object>> entries) {
        long userId = StpUtil.getLoginIdAsLong();
        userQuickEntryMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserQuickEntry>()
                        .eq(UserQuickEntry::getUserId, userId)
        );
        int order = 0;
        for (Map<String, Object> entry : entries) {
            UserQuickEntry e = new UserQuickEntry();
            e.setUserId(userId);
            e.setEntryCode((String) entry.get("code"));
            e.setEntryName((String) entry.get("name"));
            e.setEntryIcon((String) entry.get("icon"));
            e.setEntryPath((String) entry.get("path"));
            e.setSortOrder(order++);
            userQuickEntryMapper.insert(e);
        }
        return Result.ok();
    }

    @GetMapping("/banners")
    public Result<List<HomeBanner>> getBanners() {
        List<HomeBanner> banners = homeBannerMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<HomeBanner>()
                        .eq(HomeBanner::getStatus, 1)
                        .orderByAsc(HomeBanner::getSortOrder)
        );
        return Result.ok(banners);
    }
}
