package com.ruc.platform.admin.banner.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ruc.platform.admin.banner.dto.BannerSaveDTO;
import com.ruc.platform.common.api.Result;
import com.ruc.platform.home.entity.HomeBanner;
import com.ruc.platform.home.mapper.HomeBannerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
public class AdminBannerController {

    private final HomeBannerMapper homeBannerMapper;

    @GetMapping
    public Result<List<HomeBanner>> list() {
        List<HomeBanner> list = homeBannerMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<HomeBanner>()
                        .orderByAsc(HomeBanner::getSortOrder)
        );
        return Result.ok(list);
    }

    @PostMapping
    public Result<Long> create(@RequestBody BannerSaveDTO dto) {
        HomeBanner banner = new HomeBanner();
        banner.setTitle(dto.getTitle());
        banner.setSubtitle(dto.getSubtitle());
        banner.setTargetType(dto.getTargetType());
        banner.setTargetId(dto.getTargetId());
        banner.setTargetPath(dto.getTargetPath());
        banner.setSortOrder(dto.getSortOrder());
        banner.setCreatedBy(StpUtil.getLoginIdAsLong());
        banner.setUpdatedBy(StpUtil.getLoginIdAsLong());
        banner.setCreatedAt(LocalDateTime.now());
        banner.setUpdatedAt(LocalDateTime.now());
        homeBannerMapper.insert(banner);
        return Result.ok(banner.getId());
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody BannerSaveDTO dto) {
        HomeBanner banner = new HomeBanner();
        banner.setId(id);
        banner.setTitle(dto.getTitle());
        banner.setSubtitle(dto.getSubtitle());
        banner.setTargetType(dto.getTargetType());
        banner.setTargetId(dto.getTargetId());
        banner.setTargetPath(dto.getTargetPath());
        banner.setSortOrder(dto.getSortOrder());
        banner.setUpdatedBy(StpUtil.getLoginIdAsLong());
        banner.setUpdatedAt(LocalDateTime.now());
        homeBannerMapper.updateById(banner);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        homeBannerMapper.deleteById(id);
        return Result.ok();
    }
}
