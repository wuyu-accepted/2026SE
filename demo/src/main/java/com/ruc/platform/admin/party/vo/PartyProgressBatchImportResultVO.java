package com.ruc.platform.admin.party.vo;

import lombok.Data;

import java.util.List;

@Data
public class PartyProgressBatchImportResultVO {

    private Integer total;

    private Integer successCount;

    private Integer failCount;

    private List<PartyProgressImportRowVO> rows;
}
