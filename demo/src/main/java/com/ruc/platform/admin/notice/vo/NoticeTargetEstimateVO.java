package com.ruc.platform.admin.notice.vo;

import com.ruc.platform.admin.notice.dto.NoticeTargetDTO;
import lombok.Data;

@Data
public class NoticeTargetEstimateVO {

    private NoticeTargetDTO target;

    private Long targetCount;
}
