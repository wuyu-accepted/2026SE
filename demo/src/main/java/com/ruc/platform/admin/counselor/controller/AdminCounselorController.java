package com.ruc.platform.admin.counselor.controller;

import com.ruc.platform.admin.counselor.dto.CounselorCreateDTO;
import com.ruc.platform.admin.counselor.dto.CounselorUpdateDTO;
import com.ruc.platform.admin.counselor.service.AdminCounselorService;
import com.ruc.platform.admin.counselor.vo.CounselorVO;
import com.ruc.platform.common.api.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/counselors")
@RequiredArgsConstructor
public class AdminCounselorController {

    private final AdminCounselorService adminCounselorService;

    @GetMapping
    public Result<List<CounselorVO>> listAll() {
        return Result.ok(adminCounselorService.listAll());
    }

    @PostMapping
    public Result<Void> create(@RequestBody CounselorCreateDTO dto) {
        adminCounselorService.create(dto);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody CounselorUpdateDTO dto) {
        adminCounselorService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminCounselorService.delete(id);
        return Result.ok();
    }
}
