package com.ruc.platform.party.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("party_step_guidance")
public class PartyStepGuidance {

    @TableId
    private Long id;

    private String stepCode;

    private String title;

    private String content;

    private String materials;

    private Integer priority;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
