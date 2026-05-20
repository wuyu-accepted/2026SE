package com.ruc.platform.notice.service;

import com.ruc.platform.common.api.ResultCode;
import com.ruc.platform.common.exception.BizException;
import com.ruc.platform.notice.entity.UserMessage;
import com.ruc.platform.notice.mapper.UserMessageMapper;
import com.ruc.platform.notice.vo.MessageDetailVO;
import com.ruc.platform.notice.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

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

    @Override
    public List<MessageVO> getRecentMessages(Long userId, Integer limit) {
        List<UserMessage> messages = userMessageMapper.selectRecentByUserId(userId, limit);
        return messages.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public Long getUnreadCount(Long userId) {
        return userMessageMapper.countUnreadByUserId(userId);
    }

    @Override
    public MessageDetailVO getMessageDetail(Long userId, Long messageId) {
        MessageDetailVO detail = userMessageMapper.selectDetailByIdAndUserId(messageId, userId);
        if (detail == null) {
            throw new BizException(ResultCode.NOT_FOUND, "消息不存在或无权访问");
        }
        if (detail.getReadStatus() == null || detail.getReadStatus() == 0) {
            markAsRead(userId, messageId);
            detail = userMessageMapper.selectDetailByIdAndUserId(messageId, userId);
        }
        return detail;
    }

    @Override
    public void markAsRead(Long userId, Long messageId) {
        int updated = userMessageMapper.markAsReadByUserId(messageId, userId);
        if (updated == 0 && userMessageMapper.selectDetailByIdAndUserId(messageId, userId) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "消息不存在或无权访问");
        }
        log.info("标记消息为已读，userId: {}, messageId: {}", userId, messageId);
    }

    private MessageVO convertToVO(UserMessage message) {
        MessageVO vo = new MessageVO();
        BeanUtils.copyProperties(message, vo);
        return vo;
    }
}
