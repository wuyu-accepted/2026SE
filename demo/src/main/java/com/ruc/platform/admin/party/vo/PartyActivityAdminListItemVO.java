package com.ruc.platform.admin.party.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PartyActivityAdminListItemVO {

    private Long id;

    private String studentNo;

    private String realName;

    private String title;

    private LocalDate eventDate;

    private Integer status;

    private LocalDateTime submitTime;
}
