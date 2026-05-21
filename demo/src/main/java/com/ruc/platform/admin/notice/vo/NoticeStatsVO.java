package com.ruc.platform.admin.notice.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class NoticeStatsVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long noticeId;

    private Long deliveredCount;

    private Long readCount;

    private Long unreadCount;

    private Double readRate;
}
