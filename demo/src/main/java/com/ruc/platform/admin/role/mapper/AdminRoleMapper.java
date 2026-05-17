package com.ruc.platform.admin.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruc.platform.auth.entity.Role;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminRoleMapper extends BaseMapper<Role> {
}
