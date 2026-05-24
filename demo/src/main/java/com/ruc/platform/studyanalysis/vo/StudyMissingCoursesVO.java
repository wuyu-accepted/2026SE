package com.ruc.platform.studyanalysis.vo;

import lombok.Data;

import java.util.List;

@Data
public class StudyMissingCoursesVO {

    private List<String> requiredMissingCourses;

    private List<String> catalogMissingCourses;
}
