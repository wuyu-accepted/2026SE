package com.ruc.platform.studyanalysis.vo;

import lombok.Data;

import java.util.List;

@Data
public class StudyAnalysisSummaryVO {

    private String major;

    private List<StudyCategorySummaryVO> categories;

    private List<String> unknownCourses;

    private List<String> warnings;
}
