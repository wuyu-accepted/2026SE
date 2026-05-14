package com.ruc.platform.party.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("party_step_def")
public class PartyStepDef {

    @TableId
    private Long id;

    private String stageCode;

    private String stepCode;

    private String stepName;

    private Integer sortOrder;

    private String description;

    private Integer status;

    private LocalDateTime createdAt;
}
