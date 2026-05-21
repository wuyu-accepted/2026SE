package com.ruc.platform.notice.service;

import com.ruc.platform.PlatformApplication;
import com.ruc.platform.notice.entity.Notice;
import com.ruc.platform.notice.entity.UserMessage;
import com.ruc.platform.notice.mapper.NoticeMapper;
import com.ruc.platform.notice.mapper.UserMessageMapper;
import com.ruc.platform.notice.vo.MessageDetailVO;
import com.ruc.platform.notice.vo.MessageVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        userMessageMapper.deleteById(88002L);
        noticeMapper.deleteById(87001L);
        noticeMapper.deleteById(87002L);

        LocalDateTime now = LocalDateTime.now();
        insertNotice(87001L, "已读详情回归通知", "消息已读后再次点击，应继续展示完整通知正文。", now.minusMinutes(2));
        insertMessage(88001L, 1001L, 87001L, "已读详情回归通知", 1, now.minusMinutes(2));

        insertNotice(87002L, "较新的普通通知", "用于验证未置顶通知保持时间排序。", now.minusMinutes(1));
        insertMessage(88002L, 1001L, 87002L, "较新的普通通知", 0, now.minusMinutes(1));
    }

    @Test
    void returnsDetailWhenMessageIsAlreadyRead() {
        MessageDetailVO detail = messageService.getMessageDetail(1001L, 88001L);

        assertThat(detail).isNotNull();
        assertThat(detail.getId()).isEqualTo(88001L);
        assertThat(detail.getNoticeId()).isEqualTo(87001L);
        assertThat(detail.getAttachmentFileId()).isEqualTo(99001L);
        assertThat(detail.getReadStatus()).isEqualTo(1);
        assertThat(detail.getContent()).isEqualTo("消息已读后再次点击，应继续展示完整通知正文。");
    }

    @Test
    void pinsMessageWhenClientSendsNoticeId() {
        messageService.pinMessage(1001L, 87001L);

        List<MessageVO> messages = messageService.getRecentMessages(1001L, 20);

        MessageVO pinned = messages.stream()
                .filter(message -> Long.valueOf(88001L).equals(message.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(pinned.getPinnedStatus()).isEqualTo(1);
        assertThat(pinned.getPinnedTime()).isNotNull();
    }

    @Test
    void unpinsMessageWhenClientSendsNoticeId() {
        messageService.pinMessage(1001L, 88001L);
        messageService.unpinMessage(1001L, 87001L);

        List<MessageVO> messages = messageService.getRecentMessages(1001L, 20);

        MessageVO unpinned = messages.stream()
                .filter(message -> Long.valueOf(88001L).equals(message.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(unpinned.getPinnedStatus()).isEqualTo(0);
        assertThat(unpinned.getPinnedTime()).isNull();
    }

    @Test
    void recentMessagesIncludeAttachmentFileId() {
        List<MessageVO> messages = messageService.getRecentMessages(1001L, 20);

        MessageVO message = messages.stream()
                .filter(item -> Long.valueOf(88001L).equals(item.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(message.getAttachmentFileId()).isEqualTo(99001L);
    }

    @Test
    void markingAlreadyReadMessageByNoticeIdDoesNotBlockDetailNavigation() {
        messageService.markAsRead(1001L, 87001L);

        MessageDetailVO detail = messageService.getMessageDetail(1001L, 87001L);

        assertThat(detail).isNotNull();
        assertThat(detail.getId()).isEqualTo(88001L);
        assertThat(detail.getReadStatus()).isEqualTo(1);
    }

    @Test
    void pinsMessageAndOrdersPinnedMessagesFirst() {
        messageService.pinMessage(1001L, 88001L);

        List<MessageVO> messages = messageService.getRecentMessages(1001L, 20);

        assertThat(messages.get(0).getId()).isEqualTo(88001L);
        assertThat(testMessageIds(messages)).containsExactly(88001L, 88002L);
        assertThat(messages.get(0).getPinnedStatus()).isEqualTo(1);
        assertThat(messages.get(0).getPinnedTime()).isNotNull();
    }

    @Test
    void unpinsMessageAndRestoresCreatedTimeOrder() {
        messageService.pinMessage(1001L, 88001L);
        messageService.unpinMessage(1001L, 88001L);

        List<MessageVO> messages = messageService.getRecentMessages(1001L, 20);

        assertThat(testMessageIds(messages)).containsExactly(88002L, 88001L);
        MessageVO unpinned = messages.stream()
                .filter(message -> Long.valueOf(88001L).equals(message.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(unpinned.getPinnedStatus()).isEqualTo(0);
        assertThat(unpinned.getPinnedTime()).isNull();
    }

    @Test
    void cannotPinAnotherUsersMessage() {
        assertThatThrownBy(() -> messageService.pinMessage(1002L, 88001L))
                .hasMessage("消息不存在或无权访问");
    }

    private void insertNotice(Long id, String title, String content, LocalDateTime time) {
        Notice notice = new Notice();
        notice.setId(id);
        notice.setTitle(title);
        notice.setSummary("用于验证已读消息仍可打开详情");
        notice.setContent(content);
        notice.setNoticeType("教学");
        notice.setTag("回归");
        notice.setStatus(1);
        notice.setPriority(1);
        notice.setAttachmentFileId(Long.valueOf(id + 12000L));
        notice.setPublishTime(time);
        notice.setCreatedAt(time);
        notice.setUpdatedAt(time);
        noticeMapper.insert(notice);
    }

    private void insertMessage(Long id, Long userId, Long noticeId, String title, Integer readStatus, LocalDateTime time) {
        UserMessage message = new UserMessage();
        message.setId(id);
        message.setUserId(userId);
        message.setNoticeId(noticeId);
        message.setTitle(title);
        message.setSummary("用于验证已读消息仍可打开详情");
        message.setReadStatus(readStatus);
        message.setReadTime(readStatus == 1 ? time : null);
        message.setCreatedAt(time);
        userMessageMapper.insert(message);
    }

    private List<Long> testMessageIds(List<MessageVO> messages) {
        return messages.stream()
                .map(MessageVO::getId)
                .filter(id -> Long.valueOf(88001L).equals(id) || Long.valueOf(88002L).equals(id))
                .toList();
    }
}
