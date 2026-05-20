package com.ruc.platform.party.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PartyReportListItemVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String title;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long fileId;

    private Integer status;

    private String reviewComment;

    private LocalDateTime submitTime;

    private LocalDateTime reviewedAt;
}
