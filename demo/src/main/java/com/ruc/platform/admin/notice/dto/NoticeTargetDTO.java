package com.ruc.platform.admin.notice.dto;

import lombok.Data;

import java.util.List;

@Data
public class NoticeTargetDTO {

    private String grade;

    private List<String> grades;

    private String major;

    private List<String> majors;

    private String className;

    private String authType;
}
