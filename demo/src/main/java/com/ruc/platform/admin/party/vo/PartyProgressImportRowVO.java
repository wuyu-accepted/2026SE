package com.ruc.platform.admin.party.vo;

import lombok.Data;

@Data
public class PartyProgressImportRowVO {

    private Integer rowNo;

    private String studentNo;

    private String realName;

    private String stageCode;

    private String stepCode;

    private Boolean success;

    private String message;
}
