package com.ruc.platform.admin.role.controller;

import com.ruc.platform.admin.role.dto.RoleCreateDTO;
import com.ruc.platform.admin.role.dto.RoleUpdateDTO;
import com.ruc.platform.admin.role.service.AdminRoleService;
import com.ruc.platform.admin.role.vo.RoleDetailVO;
import com.ruc.platform.admin.role.vo.RoleVO;
import com.ruc.platform.common.api.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    @GetMapping
    public Result<List<RoleVO>> listAll() {
        return Result.ok(adminRoleService.listAll());
    }

    @GetMapping("/{id}")
    public Result<RoleDetailVO> getDetail(@PathVariable Long id) {
        return Result.ok(adminRoleService.getDetail(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody RoleCreateDTO dto) {
        return Result.ok(adminRoleService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody RoleUpdateDTO dto) {
        adminRoleService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminRoleService.delete(id);
        return Result.ok();
    }
}
