package com.ruc.platform.notice.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notice_feedback")
public class NoticeFeedback {

    @TableId
    private Long id;

    private Long noticeId;

    private Long messageId;

    private Long studentUserId;

    private String feedbackType;

    private String content;

    private String status;

    private Long assignedCounselorId;

    private String assignedCadreIds;

    private Long currentHandlerId;

    private Long handledBy;

    private LocalDateTime handledAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
