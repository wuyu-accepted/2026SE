package com.ruc.platform.studyanalysis.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StudyCourseUploadItemDTO {

    @NotBlank(message = "课程名称不能为空")
    private String courseName;

    @NotBlank(message = "课程大类不能为空")
    private String category;
}
