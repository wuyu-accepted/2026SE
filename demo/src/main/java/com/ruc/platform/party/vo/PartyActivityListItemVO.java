package com.ruc.platform.party.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PartyActivityListItemVO {

    private Long id;

    private String title;

    private String reason;

    private LocalDate eventDate;

    private Integer status;

    private LocalDateTime submitTime;

    private String reviewComment;

    private LocalDateTime reviewedAt;
}
