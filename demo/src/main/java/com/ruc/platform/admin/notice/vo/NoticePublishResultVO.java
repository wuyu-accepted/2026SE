package com.ruc.platform.admin.notice.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NoticePublishResultVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long noticeId;

    private Long deliveredCount;
}
