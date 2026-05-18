package com.ruc.platform.admin.party.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PartyReportVO {
    private Long id;
    private Long userId;
    private String studentNo;
    private String realName;
    private String stageCode;
    private String title;
    private String content;
    private Long fileId;
    private LocalDateTime submitTime;
    private Integer status;
    private String reviewComment;
    private Long reviewedBy;
    private String reviewerName;
    private LocalDateTime reviewedAt;
}
