package com.ruc.platform.admin.honor.controller;

import com.ruc.platform.common.api.Result;
import com.ruc.platform.honor.entity.Honor;
import com.ruc.platform.honor.mapper.HonorMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/honors")
@RequiredArgsConstructor
public class AdminHonorController {

    private final HonorMapper honorMapper;

    @GetMapping
    public Result<List<Honor>> listAll() {
        return Result.ok(honorMapper.selectList(null));
    }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody HonorCreateDTO dto) {
        Honor honor = new Honor();
        honor.setTitle(dto.getTitle());
        honor.setStudentName(dto.getStudentName());
        honor.setStudentNo(dto.getStudentNo());
        honor.setAwardLevel(dto.getAwardLevel());
        honor.setAwardDate(dto.getAwardDate());
        honor.setDescription(dto.getDescription());
        honor.setCategory(dto.getCategory());
        honor.setStatus(1);
        honor.setCreatedAt(LocalDateTime.now());
        honor.setUpdatedAt(LocalDateTime.now());
        honorMapper.insert(honor);
        log.info("创建荣誉奖励，id: {}, title: {}", honor.getId(), honor.getTitle());
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody HonorCreateDTO dto) {
        Honor honor = honorMapper.selectById(id);
        if (honor == null) {
            return Result.fail(404, "荣誉奖励不存在");
        }
        honor.setTitle(dto.getTitle());
        honor.setStudentName(dto.getStudentName());
        honor.setStudentNo(dto.getStudentNo());
        honor.setAwardLevel(dto.getAwardLevel());
        honor.setAwardDate(dto.getAwardDate());
        honor.setDescription(dto.getDescription());
        honor.setCategory(dto.getCategory());
        honor.setUpdatedAt(LocalDateTime.now());
        honorMapper.updateById(honor);
        log.info("更新荣誉奖励，id: {}", id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Honor honor = honorMapper.selectById(id);
        if (honor == null) {
            return Result.fail(404, "荣誉奖励不存在");
        }
        honorMapper.deleteById(id);
        log.info("删除荣誉奖励，id: {}", id);
        return Result.ok();
    }

    @Data
    public static class HonorCreateDTO {
        @NotBlank(message = "荣誉标题不能为空")
        private String title;

        private String studentName;

        private String studentNo;

        private String awardLevel;

        private String awardDate;

        private String description;

        private String category;
    }
}
