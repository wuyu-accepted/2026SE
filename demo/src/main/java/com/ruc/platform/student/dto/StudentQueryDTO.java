package com.ruc.platform.student.dto;

import lombok.Data;

@Data
public class StudentQueryDTO {

    private Long pageNum = 1L;

    private Long pageSize = 10L;

    private String keyword;

    private String grade;

    private String major;

    private String className;

    private String authType;
}
