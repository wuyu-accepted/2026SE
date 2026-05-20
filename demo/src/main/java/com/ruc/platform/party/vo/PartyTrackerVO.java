package com.ruc.platform.party.vo;

import lombok.Data;

import java.util.List;

@Data
public class PartyTrackerVO {

    private String currentStageCode;

    private String currentStageName;

    private String currentStepCode;

    private String currentStepName;

    private List<PartyStageVO> stages;

    private PartyStageHistoryVO currentStageHistory;

    private List<PartyGuidanceVO> guidances;
}
