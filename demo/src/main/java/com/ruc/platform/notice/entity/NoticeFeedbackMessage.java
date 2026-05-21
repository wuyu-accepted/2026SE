package com.ruc.platform.notice.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notice_feedback_message")
public class NoticeFeedbackMessage {

    @TableId
    private Long id;

    private Long feedbackId;

    private Long senderUserId;

    private String senderRole;

    private String actionType;

    private String content;

    private LocalDateTime createdAt;
}
