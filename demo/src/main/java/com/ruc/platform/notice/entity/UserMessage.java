package com.ruc.platform.notice.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户消息实体
 */
@Data
@TableName("user_message")
public class UserMessage {

    @TableId
    private Long id;

    private Long userId;

    private Long noticeId;

    private String title;

    private String summary;

    private Integer readStatus;

    private LocalDateTime readTime;

    private Integer pinnedStatus;

    private LocalDateTime pinnedTime;

    private LocalDateTime createdAt;
}
