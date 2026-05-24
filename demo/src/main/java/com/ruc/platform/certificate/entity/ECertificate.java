package com.ruc.platform.certificate.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("e_certificate")
public class ECertificate {

    @TableId
    private Long id;

    private Long userId;

    private String title;

    private String reason;

    private String templateType;

    private Integer status;

    private LocalDateTime submitTime;

    private Long approvedBy;

    private LocalDateTime approvedAt;

    private String rejectReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
