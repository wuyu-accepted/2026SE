package com.ruc.platform.student.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StudentHonorUpsertDTO {

    @NotBlank(message = "荣誉描述不能为空")
    private String content;

    private String term;

    private Long evidenceFileId;
}
