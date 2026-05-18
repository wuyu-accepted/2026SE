package com.ruc.platform.admin.party.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class PartyProgressBatchImportDTO {

    @NotEmpty(message = "导入数据不能为空")
    private List<PartyProgressImportItemDTO> items;
}
