package com.ruc.platform.party.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PartyActivityCreateDTO {

    @NotBlank(message = "活动标题不能为空")
    private String title;

    @NotBlank(message = "申请事由不能为空")
    private String reason;

    private LocalDate eventDate;

    private Long reviewerId;
}
