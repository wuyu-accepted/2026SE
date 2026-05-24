package com.ruc.platform.studyanalysis.vo;

import lombok.Data;

import java.util.List;

@Data
public class StudyCourseImportResultVO {

    private Integer totalLines;

    private Integer importedCount;

    private Integer skippedCount;

    private List<String> skippedLines;
}
