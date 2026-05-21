package com.ruc.platform.notice.service;

import com.ruc.platform.common.api.PageResult;
import com.ruc.platform.notice.dto.NoticeFeedbackCreateDTO;
import com.ruc.platform.notice.dto.NoticeFeedbackReplyDTO;
import com.ruc.platform.notice.vo.NoticeFeedbackDetailVO;
import com.ruc.platform.notice.vo.NoticeFeedbackVO;

import java.util.List;

public interface NoticeFeedbackService {

    NoticeFeedbackVO submitFeedback(Long userId, Long messageOrNoticeId, NoticeFeedbackCreateDTO dto);

    List<NoticeFeedbackVO> listStudentFeedbacks(Long userId, Long messageOrNoticeId);

    NoticeFeedbackDetailVO getStudentDetail(Long userId, Long feedbackId);

    PageResult<NoticeFeedbackVO> listCadrePending(Long cadreUserId, Long pageNum, Long pageSize);

    NoticeFeedbackDetailVO getCadreDetail(Long cadreUserId, Long feedbackId);

    void cadreReply(Long cadreUserId, Long feedbackId, NoticeFeedbackReplyDTO dto);

    void escalateToCounselor(Long cadreUserId, Long feedbackId, NoticeFeedbackReplyDTO dto);

    PageResult<NoticeFeedbackVO> listCounselorPending(Long counselorUserId, Long pageNum, Long pageSize, String feedbackType, String status, Long noticeId);

    Long countCadrePending(Long cadreUserId);

    Long countCounselorPending(Long counselorUserId);

    NoticeFeedbackDetailVO getCounselorDetail(Long counselorUserId, Long feedbackId);

    void counselorReply(Long counselorUserId, Long feedbackId, NoticeFeedbackReplyDTO dto);
}
