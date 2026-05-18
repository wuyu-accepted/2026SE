package com.ruc.platform.admin.party.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PartyActivityAdminDetailVO {

    private Long id;

    private Long userId;

    private String studentNo;

    private String realName;

    private String title;

    private String reason;

    private LocalDate eventDate;

    private Long reviewerId;

    private Integer status;

    private String reviewComment;

    private Long reviewedBy;

    private LocalDateTime reviewedAt;

    private LocalDateTime submitTime;
}
