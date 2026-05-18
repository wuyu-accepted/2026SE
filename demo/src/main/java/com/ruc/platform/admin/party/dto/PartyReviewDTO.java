package com.ruc.platform.admin.party.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PartyReviewDTO {

    @NotBlank(message = "审批意见不能为空")
    private String comment;
}
