package com.ruc.platform.notice.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class NoticeFeedbackVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long noticeId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long messageId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long studentUserId;

    private String noticeTitle;

    private String feedbackType;

    private String content;

    private String status;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long assignedCounselorId;

    private List<Long> assignedCadreIds;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long handledBy;

    private LocalDateTime handledAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
