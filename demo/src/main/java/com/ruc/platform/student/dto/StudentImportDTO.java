package com.ruc.platform.student.dto;

import lombok.Data;

@Data
public class StudentImportDTO {

    private String studentNo;

    private String realName;

    private String password;

    private String authType;

    private Integer gender;

    private String grade;

    private String major;

    private String className;

    private String politicalStatus;

    private String phone;

    private String email;

    private String hometown;

    private String dormitory;
}
