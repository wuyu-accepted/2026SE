package com.ruc.platform.notice.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageDetailVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long noticeId;

    private String title;

    private String summary;

    private String content;

    private String noticeType;

    private String tag;

    private Integer priority;

    private Integer readStatus;

    private LocalDateTime readTime;

    private LocalDateTime publishTime;

    private LocalDateTime createdAt;
}
