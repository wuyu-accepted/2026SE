package com.ruc.platform.admin.party.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PartyStageHistoryItemVO {

    private String stageCode;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String remark;
}
