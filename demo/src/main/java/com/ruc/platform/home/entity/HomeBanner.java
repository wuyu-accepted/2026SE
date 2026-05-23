package com.ruc.platform.home.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("home_banner")
public class HomeBanner {
    @TableId
    private Long id;

    private String title;

    private String subtitle;

    private String imageUrl;

    private String targetType;

    private Long targetId;

    private String targetPath;

    private String sourceType;

    private Long sourceArticleId;

    private Long sourceNoticeId;

    private Integer sortOrder;

    private Integer status;

    private Long createdBy;

    private Long updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
