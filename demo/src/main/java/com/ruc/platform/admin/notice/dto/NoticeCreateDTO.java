package com.ruc.platform.admin.notice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class NoticeCreateDTO {

    @NotBlank(message = "通知标题不能为空")
    @Size(max = 255, message = "通知标题不能超过255字")
    private String title;

    @Size(max = 500, message = "通知摘要不能超过500字")
    private String summary;

    @NotBlank(message = "通知正文不能为空")
    private String content;

    @Size(max = 32, message = "通知类型不能超过32字")
    private String noticeType;

    @Size(max = 32, message = "通知标签不能超过32字")
    private String tag;

    private Integer priority;

    private Boolean isBanner;

    private Long attachmentFileId;

    private Long feedbackCounselorId;

    private List<Long> feedbackCadreIds;

    @Valid
    private NoticeTargetDTO target;
}
