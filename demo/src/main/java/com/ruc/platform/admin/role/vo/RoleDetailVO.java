package com.ruc.platform.admin.role.vo;

import lombok.Data;

import java.util.List;

@Data
public class RoleDetailVO {
    private Long id;
    private String roleCode;
    private String roleName;
    private String description;
    private Integer status;
    private List<Long> userIds;
}
