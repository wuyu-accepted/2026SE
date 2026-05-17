package com.ruc.platform.admin.party.dto;

import lombok.Data;

@Data
public class PartyProgressImportItemDTO {

    private String studentNo;

    private String realName;

    private String stageCode;

    private String stepCode;

    private String startTime;

    private String endTime;

    private String remark;
}
