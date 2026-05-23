package com.ruc.platform.notice.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知公告实体
 */
@Data
@TableName("notice")
public class Notice {

    @TableId
    private Long id;

    private String title;

    private String summary;

    private String content;

    private String noticeType;

    private String tag;

    private Integer status;

    private LocalDateTime publishTime;

    private Integer priority;

    private Long attachmentFileId;

    private Boolean isBanner;

    private Long createdBy;

    private Long feedbackCounselorId;

    private String feedbackCadreIds;

    private String targetTags;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
