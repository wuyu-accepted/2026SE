package com.ruc.platform.notice.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeFeedbackMessageVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long feedbackId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long senderUserId;

    private String senderRole;

    private String actionType;

    private String content;

    private LocalDateTime createdAt;
}
