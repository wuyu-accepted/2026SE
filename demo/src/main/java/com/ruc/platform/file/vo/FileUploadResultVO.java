package com.ruc.platform.file.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件上传结果VO
 */
@Data
public class FileUploadResultVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String originName;

    private Long fileSize;

    private LocalDateTime uploadTime;
}
