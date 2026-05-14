package com.ruc.platform.party.vo;

import lombok.Data;

import java.util.List;

@Data
public class PartyStageVO {

    private String stageCode;

    private String stageName;

    private Integer sortOrder;

    private List<PartyStepVO> steps;
}
