package com.ruc.platform.admin.role.dto;

import lombok.Data;

@Data
public class RoleUpdateDTO {
    private String roleName;
    private String description;
    private Integer status;
}
