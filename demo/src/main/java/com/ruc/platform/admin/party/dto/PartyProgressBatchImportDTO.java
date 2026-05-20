package com.ruc.platform.admin.party.dto;

import lombok.Data;

import java.util.List;

@Data
public class PartyProgressBatchImportDTO {

    private List<PartyProgressImportItemDTO> items;
}
