package com.ruc.platform.admin.party.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PartyReportAdminListItemVO {

    private Long id;

    private String studentNo;

    private String realName;

    private String title;

    private Long fileId;

    private Integer status;

    private LocalDateTime submitTime;
}
