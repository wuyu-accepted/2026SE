package com.ruc.platform.notice.service;

import com.ruc.platform.PlatformApplication;
import com.ruc.platform.notice.entity.Notice;
import com.ruc.platform.notice.entity.UserMessage;
import com.ruc.platform.notice.mapper.NoticeMapper;
import com.ruc.platform.notice.mapper.UserMessageMapper;
import com.ruc.platform.notice.vo.MessageDetailVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PlatformApplication.class)
@ActiveProfiles("h2")
class MessageServiceImplTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private NoticeMapper noticeMapper;

    @Autowired
    private UserMessageMapper userMessageMapper;

    @BeforeEach
    void setUp() {
        userMessageMapper.deleteById(88001L);
        noticeMapper.deleteById(87001L);

        LocalDateTime now = LocalDateTime.now();
        Notice notice = new Notice();
        notice.setId(87001L);
        notice.setTitle("已读详情回归通知");
        notice.setSummary("用于验证已读消息仍可打开详情");
        notice.setContent("消息已读后再次点击，应继续展示完整通知正文。");
        notice.setNoticeType("教学");
        notice.setTag("回归");
        notice.setStatus(1);
        notice.setPriority(1);
        notice.setPublishTime(now);
        notice.setCreatedAt(now);
        notice.setUpdatedAt(now);
        noticeMapper.insert(notice);

        UserMessage message = new UserMessage();
        message.setId(88001L);
        message.setUserId(1001L);
        message.setNoticeId(87001L);
        message.setTitle("已读详情回归通知");
        message.setSummary("用于验证已读消息仍可打开详情");
        message.setReadStatus(1);
        message.setReadTime(now);
        message.setCreatedAt(now);
        userMessageMapper.insert(message);
    }

    @Test
    void returnsDetailWhenMessageIsAlreadyRead() {
        MessageDetailVO detail = messageService.getMessageDetail(1001L, 88001L);

        assertThat(detail).isNotNull();
        assertThat(detail.getId()).isEqualTo(88001L);
        assertThat(detail.getNoticeId()).isEqualTo(87001L);
        assertThat(detail.getReadStatus()).isEqualTo(1);
        assertThat(detail.getContent()).isEqualTo("消息已读后再次点击，应继续展示完整通知正文。");
    }

    @Test
    void markingAlreadyReadMessageByNoticeIdDoesNotBlockDetailNavigation() {
        messageService.markAsRead(1001L, 87001L);

        MessageDetailVO detail = messageService.getMessageDetail(1001L, 87001L);

        assertThat(detail).isNotNull();
        assertThat(detail.getId()).isEqualTo(88001L);
        assertThat(detail.getReadStatus()).isEqualTo(1);
    }
}
