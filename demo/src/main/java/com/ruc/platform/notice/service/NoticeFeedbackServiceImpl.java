package com.ruc.platform.notice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruc.platform.common.api.PageResult;
import com.ruc.platform.common.api.ResultCode;
import com.ruc.platform.common.exception.BizException;
import com.ruc.platform.notice.dto.NoticeFeedbackCreateDTO;
import com.ruc.platform.notice.dto.NoticeFeedbackReplyDTO;
import com.ruc.platform.notice.entity.Notice;
import com.ruc.platform.notice.entity.NoticeFeedback;
import com.ruc.platform.notice.entity.NoticeFeedbackMessage;
import com.ruc.platform.notice.mapper.NoticeFeedbackMapper;
import com.ruc.platform.notice.mapper.NoticeFeedbackMessageMapper;
import com.ruc.platform.notice.mapper.NoticeMapper;
import com.ruc.platform.notice.mapper.UserMessageMapper;
import com.ruc.platform.notice.vo.MessageDetailVO;
import com.ruc.platform.notice.vo.NoticeFeedbackDetailVO;
import com.ruc.platform.notice.vo.NoticeFeedbackMessageVO;
import com.ruc.platform.notice.vo.NoticeFeedbackVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.ruc.platform.auth.AuthConstants.ROLE_CADRE;
import static com.ruc.platform.auth.AuthConstants.ROLE_COUNSELOR;
import static com.ruc.platform.auth.AuthConstants.ROLE_STUDENT;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeFeedbackServiceImpl implements NoticeFeedbackService {

    public static final String TYPE_ORDINARY = "ordinary";
    public static final String TYPE_PRIVATE = "private";
    public static final String STATUS_PENDING_CADRE = "pending_cadre";
    public static final String STATUS_PENDING_COUNSELOR = "pending_counselor";
    public static final String STATUS_RESOLVED_BY_CADRE = "resolved_by_cadre";
    public static final String STATUS_RESOLVED_BY_COUNSELOR = "resolved_by_counselor";

    private final NoticeFeedbackMapper noticeFeedbackMapper;
    private final NoticeFeedbackMessageMapper noticeFeedbackMessageMapper;
    private final NoticeMapper noticeMapper;
    private final UserMessageMapper userMessageMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public NoticeFeedbackVO submitFeedback(Long userId, Long messageOrNoticeId, NoticeFeedbackCreateDTO dto) {
        MessageDetailVO message = resolveOwnedMessage(userId, messageOrNoticeId);
        Notice notice = requireNotice(message.getNoticeId());
        String feedbackType = normalizeFeedbackType(dto.getFeedbackType());
        String content = cleanRequired(dto.getContent(), "反馈内容不能为空");
        List<Long> cadreIds = parseLongIds(notice.getFeedbackCadreIds());
        Long counselorId = notice.getFeedbackCounselorId() == null ? notice.getCreatedBy() : notice.getFeedbackCounselorId();
        if (counselorId == null) {
            throw new BizException(ResultCode.BIZ_ERROR, "通知未配置最终处理辅导员");
        }

        LocalDateTime now = LocalDateTime.now();
        NoticeFeedback feedback = new NoticeFeedback();
        feedback.setNoticeId(notice.getId());
        feedback.setMessageId(message.getId());
        feedback.setStudentUserId(userId);
        feedback.setFeedbackType(feedbackType);
        feedback.setContent(content);
        feedback.setAssignedCounselorId(counselorId);
        feedback.setAssignedCadreIds(serializeLongIds(cadreIds));
        feedback.setStatus(TYPE_ORDINARY.equals(feedbackType) && !cadreIds.isEmpty()
                ? STATUS_PENDING_CADRE
                : STATUS_PENDING_COUNSELOR);
        feedback.setCreatedAt(now);
        feedback.setUpdatedAt(now);
        noticeFeedbackMapper.insert(feedback);
        insertLog(feedback.getId(), userId, ROLE_STUDENT, "submit", content, now);
        return toVO(feedback, notice);
    }

    @Override
    public List<NoticeFeedbackVO> listStudentFeedbacks(Long userId, Long messageOrNoticeId) {
        MessageDetailVO message = resolveOwnedMessage(userId, messageOrNoticeId);
        Notice notice = requireNotice(message.getNoticeId());
        return noticeFeedbackMapper.selectByStudentAndNotice(userId, notice.getId()).stream()
                .map(feedback -> toVO(feedback, notice))
                .collect(Collectors.toList());
    }

    @Override
    public NoticeFeedbackDetailVO getStudentDetail(Long userId, Long feedbackId) {
        NoticeFeedback feedback = requireFeedback(feedbackId);
        if (!Objects.equals(feedback.getStudentUserId(), userId)) {
            throw new BizException(ResultCode.FORBIDDEN, "无权查看该反馈");
        }
        return toDetailVO(feedback);
    }

    @Override
    public PageResult<NoticeFeedbackVO> listCadrePending(Long cadreUserId, Long pageNum, Long pageSize) {
        long safePageNum = normalizePageNum(pageNum);
        long safePageSize = normalizePageSize(pageSize);
        String pattern = cadreLikePattern(cadreUserId);
        Long total = safeCount(noticeFeedbackMapper.countCadrePending(pattern));
        List<NoticeFeedbackVO> records = total == 0 ? Collections.emptyList()
                : noticeFeedbackMapper.selectCadrePending(pattern, safePageSize, (safePageNum - 1) * safePageSize).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(total, safePageNum, safePageSize, records);
    }

    @Override
    public NoticeFeedbackDetailVO getCadreDetail(Long cadreUserId, Long feedbackId) {
        NoticeFeedback feedback = requireFeedback(feedbackId);
        requireCadreAccess(cadreUserId, feedback);
        return toDetailVO(feedback);
    }

    @Override
    @Transactional
    public void cadreReply(Long cadreUserId, Long feedbackId, NoticeFeedbackReplyDTO dto) {
        NoticeFeedback feedback = requireFeedback(feedbackId);
        requireCadreAccess(cadreUserId, feedback);
        if (!STATUS_PENDING_CADRE.equals(feedback.getStatus())) {
            throw new BizException(ResultCode.BIZ_ERROR, "当前反馈不可由骨干处理");
        }
        LocalDateTime now = LocalDateTime.now();
        String content = cleanRequired(dto.getContent(), "回复内容不能为空");
        feedback.setStatus(STATUS_RESOLVED_BY_CADRE);
        feedback.setHandledBy(cadreUserId);
        feedback.setHandledAt(now);
        feedback.setUpdatedAt(now);
        noticeFeedbackMapper.updateById(feedback);
        insertLog(feedbackId, cadreUserId, ROLE_CADRE, "cadre_reply", content, now);
    }

    @Override
    @Transactional
    public void escalateToCounselor(Long cadreUserId, Long feedbackId, NoticeFeedbackReplyDTO dto) {
        NoticeFeedback feedback = requireFeedback(feedbackId);
        requireCadreAccess(cadreUserId, feedback);
        if (!STATUS_PENDING_CADRE.equals(feedback.getStatus())) {
            throw new BizException(ResultCode.BIZ_ERROR, "当前反馈不可上报");
        }
        LocalDateTime now = LocalDateTime.now();
        String content = cleanRequired(dto.getContent(), "上报留言不能为空");
        feedback.setStatus(STATUS_PENDING_COUNSELOR);
        feedback.setHandledBy(cadreUserId);
        feedback.setHandledAt(now);
        feedback.setUpdatedAt(now);
        noticeFeedbackMapper.updateById(feedback);
        insertLog(feedbackId, cadreUserId, ROLE_CADRE, "escalate", content, now);
    }

    @Override
    public PageResult<NoticeFeedbackVO> listCounselorPending(Long counselorUserId, Long pageNum, Long pageSize, String feedbackType, String status, Long noticeId) {
        long safePageNum = normalizePageNum(pageNum);
        long safePageSize = normalizePageSize(pageSize);
        String normalizedType = cleanNullable(feedbackType);
        String normalizedStatus = cleanNullable(status);
        Long total = safeCount(noticeFeedbackMapper.countCounselorPendingFiltered(counselorUserId, normalizedType, normalizedStatus, noticeId));
        List<NoticeFeedbackVO> records = total == 0 ? Collections.emptyList()
                : noticeFeedbackMapper.selectCounselorPending(counselorUserId, normalizedType, normalizedStatus, noticeId, safePageSize, (safePageNum - 1) * safePageSize).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(total, safePageNum, safePageSize, records);
    }

    @Override
    public Long countCadrePending(Long cadreUserId) {
        return safeCount(noticeFeedbackMapper.countCadrePending(cadreLikePattern(cadreUserId)));
    }

    @Override
    public Long countCounselorPending(Long counselorUserId) {
        return safeCount(noticeFeedbackMapper.countCounselorPending(counselorUserId));
    }

    @Override
    public NoticeFeedbackDetailVO getCounselorDetail(Long counselorUserId, Long feedbackId) {
        NoticeFeedback feedback = requireFeedback(feedbackId);
        if (!Objects.equals(feedback.getAssignedCounselorId(), counselorUserId)) {
            throw new BizException(ResultCode.FORBIDDEN, "无权查看该反馈");
        }
        return toDetailVO(feedback);
    }

    @Override
    @Transactional
    public void counselorReply(Long counselorUserId, Long feedbackId, NoticeFeedbackReplyDTO dto) {
        NoticeFeedback feedback = requireFeedback(feedbackId);
        if (!Objects.equals(feedback.getAssignedCounselorId(), counselorUserId)) {
            throw new BizException(ResultCode.FORBIDDEN, "无权处理该反馈");
        }
        if (!STATUS_PENDING_COUNSELOR.equals(feedback.getStatus()) && !STATUS_PENDING_CADRE.equals(feedback.getStatus())) {
            throw new BizException(ResultCode.BIZ_ERROR, "当前反馈不可处理");
        }
        LocalDateTime now = LocalDateTime.now();
        String content = cleanRequired(dto.getContent(), "回复内容不能为空");
        feedback.setStatus(STATUS_RESOLVED_BY_COUNSELOR);
        feedback.setHandledBy(counselorUserId);
        feedback.setHandledAt(now);
        feedback.setUpdatedAt(now);
        noticeFeedbackMapper.updateById(feedback);
        insertLog(feedbackId, counselorUserId, ROLE_COUNSELOR, "counselor_reply", content, now);
    }

    private MessageDetailVO resolveOwnedMessage(Long userId, Long messageOrNoticeId) {
        MessageDetailVO detail = userMessageMapper.selectDetailByIdAndUserId(messageOrNoticeId, userId);
        if (detail == null) {
            detail = userMessageMapper.selectDetailByNoticeIdAndUserId(messageOrNoticeId, userId);
        }
        if (detail == null) {
            throw new BizException(ResultCode.NOT_FOUND, "消息不存在或无权访问");
        }
        return detail;
    }

    private Notice requireNotice(Long noticeId) {
        Notice notice = noticeMapper.selectById(noticeId);
        if (notice == null) {
            throw new BizException(ResultCode.NOT_FOUND, "通知不存在");
        }
        return notice;
    }

    private NoticeFeedback requireFeedback(Long feedbackId) {
        NoticeFeedback feedback = noticeFeedbackMapper.selectById(feedbackId);
        if (feedback == null) {
            throw new BizException(ResultCode.NOT_FOUND, "反馈不存在");
        }
        return feedback;
    }

    private void requireCadreAccess(Long cadreUserId, NoticeFeedback feedback) {
        if (!TYPE_ORDINARY.equals(feedback.getFeedbackType()) || !parseLongIds(feedback.getAssignedCadreIds()).contains(cadreUserId)) {
            throw new BizException(ResultCode.FORBIDDEN, "无权处理该反馈");
        }
    }

    private void insertLog(Long feedbackId, Long senderUserId, String senderRole, String actionType, String content, LocalDateTime now) {
        NoticeFeedbackMessage message = new NoticeFeedbackMessage();
        message.setFeedbackId(feedbackId);
        message.setSenderUserId(senderUserId);
        message.setSenderRole(senderRole);
        message.setActionType(actionType);
        message.setContent(cleanNullable(content));
        message.setCreatedAt(now == null ? LocalDateTime.now() : now);
        noticeFeedbackMessageMapper.insert(message);
    }

    private NoticeFeedbackVO toVO(NoticeFeedback feedback) {
        return toVO(feedback, requireNotice(feedback.getNoticeId()));
    }

    private NoticeFeedbackVO toVO(NoticeFeedback feedback, Notice notice) {
        NoticeFeedbackVO vo = new NoticeFeedbackVO();
        BeanUtils.copyProperties(feedback, vo);
        vo.setNoticeTitle(notice.getTitle());
        vo.setAssignedCadreIds(parseLongIds(feedback.getAssignedCadreIds()));
        return vo;
    }

    private NoticeFeedbackDetailVO toDetailVO(NoticeFeedback feedback) {
        Notice notice = requireNotice(feedback.getNoticeId());
        NoticeFeedbackDetailVO vo = new NoticeFeedbackDetailVO();
        BeanUtils.copyProperties(toVO(feedback, notice), vo);
        vo.setMessages(noticeFeedbackMessageMapper.selectByFeedbackId(feedback.getId()).stream()
                .map(this::toMessageVO)
                .collect(Collectors.toList()));
        return vo;
    }

    private NoticeFeedbackMessageVO toMessageVO(NoticeFeedbackMessage message) {
        NoticeFeedbackMessageVO vo = new NoticeFeedbackMessageVO();
        BeanUtils.copyProperties(message, vo);
        return vo;
    }

    private String normalizeFeedbackType(String feedbackType) {
        String cleaned = cleanRequired(feedbackType, "反馈类型不能为空");
        if (TYPE_ORDINARY.equals(cleaned) || TYPE_PRIVATE.equals(cleaned)) {
            return cleaned;
        }
        throw new BizException(ResultCode.PARAM_ERROR, "反馈类型仅支持 ordinary 或 private");
    }

    private List<Long> parseLongIds(String rawIds) {
        if (rawIds == null || rawIds.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<Long> parsed = objectMapper.readValue(rawIds, new TypeReference<List<Long>>() {});
            List<Long> normalized = new ArrayList<>();
            if (parsed != null) {
                for (Long id : parsed) {
                    if (id != null && !normalized.contains(id)) {
                        normalized.add(id);
                    }
                }
            }
            return normalized;
        } catch (JsonProcessingException e) {
            log.warn("反馈骨干列表解析失败，rawIds: {}", rawIds, e);
            return Collections.emptyList();
        }
    }

    private String serializeLongIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (JsonProcessingException e) {
            throw new BizException(ResultCode.SYSTEM_ERROR, "反馈骨干列表序列化失败");
        }
    }

    private String cadreLikePattern(Long cadreUserId) {
        return "%" + cadreUserId + "%";
    }

    private long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum < 1 ? 1L : pageNum;
    }

    private long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10L;
        }
        return Math.min(pageSize, 100L);
    }

    private Long safeCount(Long count) {
        return count == null ? 0L : count;
    }

    private String cleanRequired(String value, String message) {
        String cleaned = cleanNullable(value);
        if (cleaned == null) {
            throw new BizException(ResultCode.PARAM_ERROR, message);
        }
        return cleaned;
    }

    private String cleanNullable(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
