package com.ruc.platform.studyanalysis.vo;

import lombok.Data;

import java.util.List;

@Data
public class StudyElectiveModuleStatusVO {

    private String key;

    private String name;

    private Integer totalCourses;

    private Integer takenCourses;

    private Integer missingCourses;

    private List<String> missingPreview;
}
