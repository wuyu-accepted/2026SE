package com.ruc.platform.party.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PartyActivityListItemVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String title;

    private String reason;

    private LocalDate eventDate;

    private Integer status;

    private LocalDateTime submitTime;

    private String reviewComment;

    private LocalDateTime reviewedAt;
}
