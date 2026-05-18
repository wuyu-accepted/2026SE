package com.ruc.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AccountLoginDTO {

    @NotBlank(message = "账号不能为空")
    private String studentNo;

    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 登录端：miniprogram/web
     */
    private String clientType;
}
