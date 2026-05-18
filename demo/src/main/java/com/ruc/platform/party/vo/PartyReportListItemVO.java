package com.ruc.platform.party.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PartyReportListItemVO {

    private Long id;

    private String title;

    private Long fileId;

    private Integer status;

    private String reviewComment;

    private LocalDateTime submitTime;

    private LocalDateTime reviewedAt;
}
