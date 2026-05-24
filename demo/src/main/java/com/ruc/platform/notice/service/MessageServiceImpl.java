package com.ruc.platform.notice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruc.platform.admin.notice.dto.NoticeTargetDTO;
import com.ruc.platform.common.api.ResultCode;
import com.ruc.platform.common.exception.BizException;
import com.ruc.platform.notice.entity.UserMessage;
import com.ruc.platform.notice.mapper.UserMessageMapper;
import com.ruc.platform.notice.vo.MessageDetailVO;
import com.ruc.platform.notice.vo.MessageVO;
import com.ruc.platform.student.entity.StudentProfile;
import com.ruc.platform.student.mapper.StudentProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final UserMessageMapper userMessageMapper;
    private final com.ruc.platform.notice.mapper.NoticeMapper noticeMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<MessageVO> getRecentMessages(Long userId, Integer limit) {
        List<UserMessage> messages = userMessageMapper.selectRecentByUserId(userId, limit);
        return messages.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }


    @Override
    public List<MessageVO> listMessages(Long userId, String keyword, String sortBy, Integer limit) {
        int safeLimit = limit == null || limit <= 0 ? 100 : Math.min(limit, 200);
        String safeKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        boolean sortByRelevance = "relevance".equalsIgnoreCase(sortBy);
        autoSeedMessagesIfEmpty(userId);
        return userMessageMapper.selectAllByUserId(userId).stream()
                .map(this::convertToVO)
                .filter(message -> safeKeyword.isBlank() || matchesKeyword(message, safeKeyword))
                .sorted(sortByRelevance ? relevanceComparator(safeKeyword) : timeComparator())
                .limit(safeLimit)
                .collect(Collectors.toList());
    }

    private void autoSeedMessagesIfEmpty(Long userId) {
        List<Long> existingNoticeIds = userMessageMapper.selectObjs(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserMessage>()
                        .select(UserMessage::getNoticeId)
                        .eq(UserMessage::getUserId, userId)
        );
        List<com.ruc.platform.notice.entity.Notice> allNotices = noticeMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.ruc.platform.notice.entity.Notice>()
                        .eq(com.ruc.platform.notice.entity.Notice::getStatus, 1)
        );
        if (allNotices == null) return;

        StudentProfile profile = studentProfileMapper.selectByUserId(userId);

        for (com.ruc.platform.notice.entity.Notice notice : allNotices) {
            if (existingNoticeIds.contains(notice.getId())) {
                continue;
            }
            if (!matchesTarget(notice.getTargetTags(), profile)) {
                continue;
            }
            UserMessage msg = new UserMessage();
            msg.setUserId(userId);
            msg.setNoticeId(notice.getId());
            msg.setTitle(notice.getTitle());
            msg.setSummary(notice.getSummary());
            msg.setReadStatus(0);
            userMessageMapper.insert(msg);
        }
    }

    private boolean matchesTarget(String targetTags, StudentProfile profile) {
        if (!StringUtils.hasText(targetTags)) {
            return true;
        }
        NoticeTargetDTO target;
        try {
            target = objectMapper.readValue(targetTags, NoticeTargetDTO.class);
        } catch (JsonProcessingException e) {
            log.warn("解析 targetTags 失败: {}", targetTags, e);
            return true;
        }
        if (profile == null) {
            return false;
        }
        if (hasCollection(target.getGrades())) {
            if (profile.getGrade() == null || !target.getGrades().contains(profile.getGrade())) {
                return false;
            }
        }
        if (hasCollection(target.getMajors())) {
            if (profile.getMajor() == null || !target.getMajors().contains(profile.getMajor())) {
                return false;
            }
        }
        if (StringUtils.hasText(target.getClassName())) {
            if (!target.getClassName().equals(profile.getClassName())) {
                return false;
            }
        }
        if (StringUtils.hasText(target.getAuthType())) {
            if (!target.getAuthType().equals(profile.getAuthType())) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasCollection(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }

    @Override
    public Long getUnreadCount(Long userId) {
        return userMessageMapper.countUnreadByUserId(userId);
    }

    @Override
    public List<MessageDetailVO> searchMessages(Long userId, String keyword, Integer limit) {
        int safeLimit = limit == null || limit <= 0 ? 20 : Math.min(limit, 50);
        String safeKeyword = keyword == null ? "" : keyword.trim();
        if (safeKeyword.isBlank()) {
            return getRecentMessages(userId, safeLimit).stream().map(message -> {
                MessageDetailVO detail = new MessageDetailVO();
                BeanUtils.copyProperties(message, detail);
                return detail;
            }).collect(Collectors.toList());
        }
        return userMessageMapper.searchByKeyword(userId, safeKeyword, safeLimit);
    }


    @Override
    public MessageDetailVO getMessageDetail(Long userId, Long messageId) {
        MessageDetailVO detail = userMessageMapper.selectDetailByIdAndUserId(messageId, userId);
        if (detail == null) {
            detail = userMessageMapper.selectDetailByNoticeIdAndUserId(messageId, userId);
        }
        if (detail == null) {
            throw new BizException(ResultCode.NOT_FOUND, "消息不存在或无权访问");
        }
        if (detail.getReadStatus() == null || detail.getReadStatus() == 0) {
            markAsRead(userId, detail.getId());
            detail = userMessageMapper.selectDetailByIdAndUserId(detail.getId(), userId);
        }
        return detail;
    }

    @Override
    public void markAsRead(Long userId, Long messageId) {
        int updated = userMessageMapper.markAsReadByUserId(messageId, userId);
        if (updated > 0) {
            log.info("标记消息为已读，userId: {}, messageId: {}", userId, messageId);
            return;
        }

        MessageDetailVO detail = userMessageMapper.selectDetailByIdAndUserId(messageId, userId);
        if (detail == null) {
            detail = userMessageMapper.selectDetailByNoticeIdAndUserId(messageId, userId);
        }
        if (detail == null) {
            throw new BizException(ResultCode.NOT_FOUND, "消息不存在或无权访问");
        }
        if (detail.getReadStatus() == null || detail.getReadStatus() == 0) {
            userMessageMapper.markAsReadByUserId(detail.getId(), userId);
        }
        log.info("标记消息为已读，userId: {}, messageId: {}, resolvedMessageId: {}", userId, messageId, detail.getId());
    }

    @Override
    public void pinMessage(Long userId, Long messageId) {
        int updated = userMessageMapper.pinByUserId(messageId, userId);
        if (updated == 0) {
            updated = userMessageMapper.pinByNoticeIdAndUserId(messageId, userId);
        }
        if (updated == 0) {
            throw new BizException(ResultCode.NOT_FOUND, "消息不存在或无权访问");
        }
        log.info("置顶消息，userId: {}, messageId: {}", userId, messageId);
    }

    @Override
    public void unpinMessage(Long userId, Long messageId) {
        int updated = userMessageMapper.unpinByUserId(messageId, userId);
        if (updated == 0) {
            updated = userMessageMapper.unpinByNoticeIdAndUserId(messageId, userId);
        }
        if (updated == 0) {
            throw new BizException(ResultCode.NOT_FOUND, "消息不存在或无权访问");
        }
        log.info("取消置顶消息，userId: {}, messageId: {}", userId, messageId);
    }


    private Comparator<MessageVO> timeComparator() {
        return Comparator.comparing(MessageVO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private Comparator<MessageVO> relevanceComparator(String keyword) {
        return Comparator.comparingInt((MessageVO message) -> relevanceScore(message, keyword)).reversed()
                .thenComparing(timeComparator());
    }

    private int relevanceScore(MessageVO message, String keyword) {
        int score = 0;
        if (message.getPinnedStatus() != null && message.getPinnedStatus() == 1) {
            score += 100;
        }
        if (message.getReadStatus() != null && message.getReadStatus() == 0) {
            score += 20;
        }
        if (!keyword.isBlank()) {
            String title = lower(message.getTitle());
            String summary = lower(message.getSummary());
            if (title.equals(keyword)) {
                score += 80;
            } else if (title.contains(keyword)) {
                score += 50;
            }
            if (summary.contains(keyword)) {
                score += 20;
            }
        }
        return score;
    }

    private boolean matchesKeyword(MessageVO message, String keyword) {
        return lower(message.getTitle()).contains(keyword) || lower(message.getSummary()).contains(keyword);
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase();
    }


    private MessageVO convertToVO(UserMessage message) {
        MessageVO vo = new MessageVO();
        BeanUtils.copyProperties(message, vo);
        if (vo.getPinnedStatus() == null) {
            vo.setPinnedStatus(0);
        }
        return vo;
    }
}
