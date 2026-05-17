package com.ruc.platform.admin.role.service;

import cn.dev33.satoken.stp.StpUtil;
import com.ruc.platform.admin.role.dto.RoleCreateDTO;
import com.ruc.platform.admin.role.dto.RoleUpdateDTO;
import com.ruc.platform.admin.role.mapper.AdminRoleMapper;
import com.ruc.platform.admin.role.mapper.AdminUserRoleMapper;
import com.ruc.platform.admin.role.vo.RoleDetailVO;
import com.ruc.platform.admin.role.vo.RoleVO;
import com.ruc.platform.auth.entity.Role;
import com.ruc.platform.common.api.ResultCode;
import com.ruc.platform.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRoleServiceImpl implements AdminRoleService {

    private final AdminRoleMapper adminRoleMapper;
    private final AdminUserRoleMapper adminUserRoleMapper;

    @Override
    public List<RoleVO> listAll() {
        return adminRoleMapper.selectList(null).stream().map(r -> {
            RoleVO vo = new RoleVO();
            vo.setId(r.getId());
            vo.setRoleCode(r.getRoleCode());
            vo.setRoleName(r.getRoleName());
            vo.setDescription(r.getDescription());
            vo.setStatus(r.getStatus());
            vo.setCreatedAt(r.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public RoleDetailVO getDetail(Long id) {
        Role role = adminRoleMapper.selectById(id);
        if (role == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "角色不存在");
        }
        RoleDetailVO vo = new RoleDetailVO();
        vo.setId(role.getId());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setDescription(role.getDescription());
        vo.setStatus(role.getStatus());
        vo.setUserIds(adminUserRoleMapper.selectUserIdsByRoleId(id));
        return vo;
    }

    @Override
    @Transactional
    public Long create(RoleCreateDTO dto) {
        Role role = new Role();
        role.setId(StpUtil.getLoginIdAsLong());
        role.setRoleCode(dto.getRoleCode());
        role.setRoleName(dto.getRoleName());
        role.setDescription(dto.getDescription());
        role.setStatus(1);
        role.setCreatedAt(LocalDateTime.now());
        adminRoleMapper.insert(role);
        log.info("新增角色，id: {}, roleCode: {}", role.getId(), dto.getRoleCode());
        return role.getId();
    }

    @Override
    @Transactional
    public void update(Long id, RoleUpdateDTO dto) {
        Role role = adminRoleMapper.selectById(id);
        if (role == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "角色不存在");
        }
        role.setRoleName(dto.getRoleName());
        role.setDescription(dto.getDescription());
        role.setStatus(dto.getStatus());
        adminRoleMapper.updateById(role);
        log.info("更新角色，id: {}", id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Role role = adminRoleMapper.selectById(id);
        if (role == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "角色不存在");
        }
        adminRoleMapper.deleteById(id);
        log.info("删除角色，id: {}", id);
    }
}
