package com.ruc.platform.party.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 思想汇报实体
 */
@Data
@TableName("party_report")
public class PartyReport {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String stageCode;

    private String title;

    private String content;

    private Long fileId;

    private LocalDateTime submitTime;

    private Integer status;

    private String reviewComment;

    private Long reviewedBy;

    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt;
}
