package com.ruc.platform.student.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudentHonorVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String title;

    private String term;

    private String description;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long evidenceFileId;

    private String evidenceDownloadUrl;

    private LocalDateTime createdAt;
}
