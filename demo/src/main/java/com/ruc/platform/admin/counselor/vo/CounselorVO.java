package com.ruc.platform.admin.counselor.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CounselorVO {
    private Long id;
    private String realName;
    private String studentNo;
    private String phone;
    private Integer status;
    private LocalDateTime createdAt;
}
