package com.ruc.platform.admin.party.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PartyActivityVO {
    private Long id;
    private Long userId;
    private String studentNo;
    private String realName;
    private String title;
    private String reason;
    private String eventDate;
    private Integer status;
    private String reviewComment;
    private String reviewerName;
    private LocalDateTime submitTime;
    private LocalDateTime reviewedAt;
}
