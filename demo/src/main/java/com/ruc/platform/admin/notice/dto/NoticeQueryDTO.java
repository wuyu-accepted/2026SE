package com.ruc.platform.admin.notice.dto;

import lombok.Data;

@Data
public class NoticeQueryDTO {

    private Long pageNum = 1L;

    private Long pageSize = 10L;

    private String keyword;

    private String noticeType;

    private Integer status;
}
