package com.ruc.platform.admin.role.service;

import com.ruc.platform.admin.role.dto.RoleCreateDTO;
import com.ruc.platform.admin.role.dto.RoleUpdateDTO;
import com.ruc.platform.admin.role.vo.RoleDetailVO;
import com.ruc.platform.admin.role.vo.RoleVO;

import java.util.List;

public interface AdminRoleService {
    List<RoleVO> listAll();
    RoleDetailVO getDetail(Long id);
    Long create(RoleCreateDTO dto);
    void update(Long id, RoleUpdateDTO dto);
    void delete(Long id);
}
