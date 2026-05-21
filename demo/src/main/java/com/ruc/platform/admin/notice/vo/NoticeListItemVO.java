package com.ruc.platform.admin.notice.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ruc.platform.admin.notice.dto.NoticeTargetDTO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class NoticeListItemVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String title;

    private String summary;

    private String noticeType;

    private String tag;

    private Integer status;

    private Integer priority;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long attachmentFileId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long feedbackCounselorId;

    private List<Long> feedbackCadreIds;

    private LocalDateTime publishTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long deliveredCount;

    private NoticeTargetDTO target;
}
