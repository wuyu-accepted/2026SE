package com.ruc.platform.admin.party.vo;

import lombok.Data;

import java.util.List;

@Data
public class PartyStudentProgressAdminVO {

    private Long userId;

    private String studentNo;

    private String realName;

    private String currentStageCode;

    private String currentStepCode;

    private List<PartyStageHistoryItemVO> stageHistories;
}
