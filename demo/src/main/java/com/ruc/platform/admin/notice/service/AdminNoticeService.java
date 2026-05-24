package com.ruc.platform.admin.notice.service;

import com.ruc.platform.admin.notice.dto.NoticeCreateDTO;
import com.ruc.platform.admin.notice.dto.NoticeQueryDTO;
import com.ruc.platform.admin.notice.dto.NoticeUpdateDTO;
import com.ruc.platform.admin.notice.vo.NoticeDetailVO;
import com.ruc.platform.admin.notice.vo.NoticeListItemVO;
import com.ruc.platform.admin.notice.vo.NoticePublishResultVO;
import com.ruc.platform.admin.notice.vo.NoticeStatsVO;
import com.ruc.platform.admin.notice.vo.NoticeTargetEstimateVO;
import com.ruc.platform.common.api.PageResult;

public interface AdminNoticeService {

    PageResult<NoticeListItemVO> listNotices(NoticeQueryDTO queryDTO);

    NoticeDetailVO getNoticeDetail(Long id);

    NoticeDetailVO createNotice(Long creatorId, NoticeCreateDTO createDTO);

    NoticeDetailVO updateNotice(Long id, NoticeUpdateDTO updateDTO);

    NoticePublishResultVO publishNotice(Long id, Long publisherId);

    NoticeTargetEstimateVO estimateTarget(Long id);

    NoticeStatsVO getNoticeStats(Long id);

    void offlineNotice(Long id);

    void deleteNotice(Long id);
}
