package com.ruc.platform.party.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PartyStageHistoryVO {

    private String stageCode;

    private String stageName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String remark;
}
