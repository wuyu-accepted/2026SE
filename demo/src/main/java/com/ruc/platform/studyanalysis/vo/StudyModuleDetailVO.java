package com.ruc.platform.studyanalysis.vo;

import lombok.Data;

import java.util.List;

@Data
public class StudyModuleDetailVO {

    private String module;

    private List<String> missingCourses;

    private List<StudyElectiveModuleStatusVO> electiveModules;

    private List<ElectiveSuggestionVO> electiveSuggestions;

    private List<SemesterPlanVO> semesterPlans;

    @Data
    public static class ElectiveSuggestionVO {
        private String key;
        private String name;
        private String reason;
        private List<String> recommendedCourses;
    }

    @Data
    public static class SemesterPlanVO {
        private String termKey;
        private String termLabel;
        private List<String> requiredCourses;
        private List<String> electiveCourses;
    }
}
