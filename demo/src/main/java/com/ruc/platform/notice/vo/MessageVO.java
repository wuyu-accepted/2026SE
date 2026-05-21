package com.ruc.platform.notice.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息VO
 */
@Data
public class MessageVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long noticeId;

    private String title;

    private String summary;

    private Integer readStatus;

    private LocalDateTime readTime;

    private Integer pinnedStatus;

    private LocalDateTime pinnedTime;

    private LocalDateTime createdAt;
}
