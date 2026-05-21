package com.ruc.platform.notice.service;

import com.ruc.platform.notice.vo.MessageDetailVO;
import com.ruc.platform.notice.vo.MessageVO;

import java.util.List;

/**
 * 消息服务接口
 */
public interface MessageService {

    /**
     * 获取最近消息
     */
    List<MessageVO> getRecentMessages(Long userId, Integer limit);

    /**
     * 获取未读消息数量
     */
    Long getUnreadCount(Long userId);

    /**
     * 获取消息详情
     */
    MessageDetailVO getMessageDetail(Long userId, Long messageId);

    /**
     * 标记消息为已读
     */
    void markAsRead(Long userId, Long messageId);

    /**
     * 置顶消息
     */
    void pinMessage(Long userId, Long messageId);

    /**
     * 取消置顶消息
     */
    void unpinMessage(Long userId, Long messageId);
}
