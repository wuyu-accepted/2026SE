package com.ruc.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AccountRegisterDTO {

    @NotBlank(message = "姓名不能为空")
    private String realName;

    @NotBlank(message = "学号不能为空")
    private String studentNo;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    private String phone;

    private String email;

    private Integer gender;

    private String grade;

    private String major;

    private String className;

    private String politicalStatus;

    private String bio;

    private String hometown;

    private String dormitory;

    private String authType;

    /**
     * 注册端：miniprogram/web
     */
    private String clientType;
}
