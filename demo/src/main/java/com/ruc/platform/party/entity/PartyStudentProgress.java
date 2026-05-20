package com.ruc.platform.party.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生党团进度实体
 */
@Data
@TableName("party_student_progress")
public class PartyStudentProgress {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String currentStageCode;

    private String currentStepCode;

    private LocalDateTime updatedAt;
}
