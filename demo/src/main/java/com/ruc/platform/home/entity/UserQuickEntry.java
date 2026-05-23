package com.ruc.platform.home.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_quick_entry")
public class UserQuickEntry {
    @TableId
    private Long id;

    private Long userId;

    private String entryCode;

    private String entryName;

    private String entryIcon;

    private String entryPath;

    private Integer sortOrder;

    private LocalDateTime createdAt;
}
