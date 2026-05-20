package com.ruc.platform.auth.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserVO {

    private Long id;

    private String realName;

    private String studentNo;

    private String accountType;

    private List<String> roles;

    private String authType;

    private String className;

    private String avatarUrl;
}
