package com.ruc.platform.student.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudentListItemVO {

    private Long id;

    private Long userId;

    private String studentNo;

    private String realName;

    private String phone;

    private String email;

    private Integer status;

    private Integer gender;

    private String grade;

    private String major;

    private String className;

    private String politicalStatus;

    private String authType;

    private String hometown;

    private String dormitory;

    private LocalDateTime updatedAt;
}
