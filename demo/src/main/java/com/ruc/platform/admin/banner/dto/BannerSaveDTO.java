package com.ruc.platform.admin.banner.dto;

import lombok.Data;

@Data
public class BannerSaveDTO {
    private String title;
    private String subtitle;
    private String targetType;
    private Long targetId;
    private String targetPath;
    private Integer sortOrder;
}
