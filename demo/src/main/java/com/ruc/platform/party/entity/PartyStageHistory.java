package com.ruc.platform.party.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("party_stage_history")
public class PartyStageHistory {

    @TableId
    private Long id;

    private Long userId;

    private String stageCode;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String remark;

    private Long updatedBy;

    private LocalDateTime updatedAt;

    private LocalDateTime createdAt;
}
