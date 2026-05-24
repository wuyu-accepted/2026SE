package com.ruc.platform.notice.service;

import com.ruc.platform.PlatformApplication;
import com.ruc.platform.common.exception.BizException;
import com.ruc.platform.notice.dto.NoticeFeedbackCreateDTO;
import com.ruc.platform.notice.dto.NoticeFeedbackReplyDTO;
import com.ruc.platform.notice.entity.Notice;
import com.ruc.platform.notice.entity.NoticeFeedback;
import com.ruc.platform.notice.entity.NoticeFeedbackMessage;
import com.ruc.platform.notice.entity.UserMessage;
import com.ruc.platform.notice.mapper.NoticeFeedbackMapper;
import com.ruc.platform.notice.mapper.NoticeFeedbackMessageMapper;
import com.ruc.platform.notice.mapper.NoticeMapper;
import com.ruc.platform.notice.mapper.UserMessageMapper;
import com.ruc.platform.notice.vo.NoticeFeedbackDetailVO;
import com.ruc.platform.notice.vo.NoticeFeedbackVO;
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
class NoticeFeedbackServiceImplTest {

    @Autowired
    private NoticeFeedbackService noticeFeedbackService;

    @Autowired
    private NoticeMapper noticeMapper;

    @Autowired
    private UserMessageMapper userMessageMapper;

    @Autowired
    private NoticeFeedbackMapper noticeFeedbackMapper;

    @Autowired
    private NoticeFeedbackMessageMapper noticeFeedbackMessageMapper;

    @BeforeEach
    void setUp() {
        noticeFeedbackMessageMapper.delete(null);
        noticeFeedbackMapper.delete(null);
        userMessageMapper.deleteById(89101L);
        userMessageMapper.deleteById(89102L);
        userMessageMapper.deleteById(89103L);
        noticeMapper.deleteById(88101L);
        noticeMapper.deleteById(88102L);
        noticeMapper.deleteById(88103L);

        LocalDateTime now = LocalDateTime.now();
        insertNotice(88101L, 5001L, "[2002,2003]", now.minusMinutes(5));
        insertMessage(89101L, 1001L, 88101L, now.minusMinutes(4));
        insertNotice(88102L, 5002L, null, now.minusMinutes(3));
        insertMessage(89102L, 1002L, 88102L, now.minusMinutes(2));
        insertNoticeWithCreatorAndFeedbackCounselor(88103L, 2002L, null, null, now.minusMinutes(1));
        insertMessage(89103L, 1001L, 88103L, now);
    }

    @Test
    void ordinaryFeedbackRoutesToAssignedCadresAndWritesSubmitLog() {
        NoticeFeedbackCreateDTO dto = new NoticeFeedbackCreateDTO();
        dto.setFeedbackType("ordinary");
        dto.setContent("报名入口在哪里？");

        NoticeFeedbackVO feedback = noticeFeedbackService.submitFeedback(1001L, 89101L, dto);

        assertThat(feedback.getNoticeId()).isEqualTo(88101L);
        assertThat(feedback.getFeedbackType()).isEqualTo("ordinary");
        assertThat(feedback.getStatus()).isEqualTo("pending_cadre");
        assertThat(feedback.getAssignedCounselorId()).isEqualTo(5001L);
        assertThat(feedback.getAssignedCadreIds()).containsExactly(2002L, 2003L);
        List<NoticeFeedbackMessage> logs = noticeFeedbackMessageMapper.selectByFeedbackId(feedback.getId());
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getActionType()).isEqualTo("submit");
    }

    @Test
    void privateFeedbackRoutesDirectlyToCounselorAndIsHiddenFromCadre() {
        NoticeFeedbackCreateDTO dto = new NoticeFeedbackCreateDTO();
        dto.setFeedbackType("private");
        dto.setContent("想私下咨询。");

        NoticeFeedbackVO feedback = noticeFeedbackService.submitFeedback(1001L, 89101L, dto);

        assertThat(feedback.getStatus()).isEqualTo("pending_counselor");
        assertThat(feedback.getAssignedCounselorId()).isEqualTo(5001L);
        assertThat(feedback.getAssignedCadreIds()).isEmpty();
        assertThat(noticeFeedbackService.listCadrePending(2002L, 1L, 10L).getRecords()).isEmpty();
        assertThatThrownBy(() -> noticeFeedbackService.getCadreDetail(2002L, feedback.getId()))
                .isInstanceOf(BizException.class)
                .hasMessage("无权处理该反馈");
    }

    @Test
    void ordinaryFeedbackWithoutCadresRoutesToCounselor() {
        NoticeFeedbackCreateDTO dto = new NoticeFeedbackCreateDTO();
        dto.setFeedbackType("ordinary");
        dto.setContent("没有骨干时谁处理？");

        NoticeFeedbackVO feedback = noticeFeedbackService.submitFeedback(1002L, 88102L, dto);

        assertThat(feedback.getStatus()).isEqualTo("pending_counselor");
        assertThat(feedback.getAssignedCounselorId()).isEqualTo(5002L);
    }

    @Test
    void feedbackWithoutConfiguredCounselorFailsUntilNoticeIsPublishedByOwner() {
        NoticeFeedbackCreateDTO dto = new NoticeFeedbackCreateDTO();
        dto.setFeedbackType("private");
        dto.setContent("演示通知缺少辅导员配置。");

        assertThatThrownBy(() -> noticeFeedbackService.submitFeedback(1001L, 89103L, dto))
                .isInstanceOf(BizException.class)
                .hasMessage("通知未配置最终处理辅导员");
    }

    @Test
    void cannotSubmitFeedbackForAnotherUsersMessage() {
        NoticeFeedbackCreateDTO dto = new NoticeFeedbackCreateDTO();
        dto.setFeedbackType("ordinary");
        dto.setContent("越权反馈。");

        assertThatThrownBy(() -> noticeFeedbackService.submitFeedback(1002L, 89101L, dto))
                .isInstanceOf(BizException.class)
                .hasMessage("消息不存在或无权访问");
    }

    @Test
    void assignedCadreCanReplyAndCounselorCanInspectLog() {
        NoticeFeedbackVO feedback = submitOrdinaryFeedback();
        NoticeFeedbackReplyDTO replyDTO = new NoticeFeedbackReplyDTO();
        replyDTO.setContent("入口在服务页。");

        noticeFeedbackService.cadreReply(2002L, feedback.getId(), replyDTO);

        NoticeFeedback stored = noticeFeedbackMapper.selectById(feedback.getId());
        assertThat(stored.getStatus()).isEqualTo("resolved_by_cadre");
        NoticeFeedbackDetailVO detail = noticeFeedbackService.getCounselorDetail(5001L, feedback.getId());
        assertThat(detail.getMessages()).extracting("actionType").containsExactly("submit", "cadre_reply");
        assertThat(detail.getMessages().get(1).getSenderUserId()).isEqualTo(2002L);
    }

    @Test
    void counselorPendingListIncludesResolvedCadreFeedbackForInspection() {
        NoticeFeedbackVO feedback = submitOrdinaryFeedback();
        NoticeFeedbackReplyDTO replyDTO = new NoticeFeedbackReplyDTO();
        replyDTO.setContent("入口在服务页。");
        noticeFeedbackService.cadreReply(2002L, feedback.getId(), replyDTO);

        assertThat(noticeFeedbackService.listCounselorPending(5001L, 1L, 10L, null, "resolved_by_cadre", null).getRecords())
                .extracting("id")
                .contains(feedback.getId());
    }

    @Test
    void assignedCadreCanEscalateWithComment() {
        NoticeFeedbackVO feedback = submitOrdinaryFeedback();
        NoticeFeedbackReplyDTO replyDTO = new NoticeFeedbackReplyDTO();
        replyDTO.setContent("需要辅导员确认。 ");

        noticeFeedbackService.escalateToCounselor(2003L, feedback.getId(), replyDTO);

        NoticeFeedback stored = noticeFeedbackMapper.selectById(feedback.getId());
        assertThat(stored.getStatus()).isEqualTo("pending_counselor");
        NoticeFeedbackDetailVO detail = noticeFeedbackService.getCounselorDetail(5001L, feedback.getId());
        assertThat(detail.getMessages()).extracting("actionType").containsExactly("submit", "escalate");
        assertThat(detail.getMessages().get(1).getContent()).isEqualTo("需要辅导员确认。");
    }

    @Test
    void unassignedCadreCannotAct() {
        NoticeFeedbackVO feedback = submitOrdinaryFeedback();
        NoticeFeedbackReplyDTO replyDTO = new NoticeFeedbackReplyDTO();
        replyDTO.setContent("越权处理。");

        assertThatThrownBy(() -> noticeFeedbackService.cadreReply(2999L, feedback.getId(), replyDTO))
                .isInstanceOf(BizException.class)
                .hasMessage("无权处理该反馈");
    }

    @Test
    void counselorCanReplyToPrivateFeedback() {
        NoticeFeedbackCreateDTO dto = new NoticeFeedbackCreateDTO();
        dto.setFeedbackType("private");
        dto.setContent("私密反馈。");
        NoticeFeedbackVO feedback = noticeFeedbackService.submitFeedback(1001L, 89101L, dto);
        NoticeFeedbackReplyDTO replyDTO = new NoticeFeedbackReplyDTO();
        replyDTO.setContent("已私下沟通。");

        noticeFeedbackService.counselorReply(5001L, feedback.getId(), replyDTO);

        NoticeFeedback stored = noticeFeedbackMapper.selectById(feedback.getId());
        assertThat(stored.getStatus()).isEqualTo("resolved_by_counselor");
        NoticeFeedbackDetailVO detail = noticeFeedbackService.getStudentDetail(1001L, feedback.getId());
        assertThat(detail.getMessages()).extracting("actionType").containsExactly("submit", "counselor_reply");
    }

    private NoticeFeedbackVO submitOrdinaryFeedback() {
        NoticeFeedbackCreateDTO dto = new NoticeFeedbackCreateDTO();
        dto.setFeedbackType("ordinary");
        dto.setContent("报名入口在哪里？");
        return noticeFeedbackService.submitFeedback(1001L, 89101L, dto);
    }

    private void insertNotice(Long id, Long counselorId, String cadreIds, LocalDateTime time) {
        insertNoticeWithCreatorAndFeedbackCounselor(id, counselorId, counselorId, cadreIds, time);
    }

    private void insertNoticeWithCreatorAndFeedbackCounselor(Long id, Long creatorId, Long counselorId, String cadreIds, LocalDateTime time) {
        Notice notice = new Notice();
        notice.setId(id);
        notice.setTitle("反馈测试通知" + id);
        notice.setSummary("用于反馈测试");
        notice.setContent("通知正文");
        notice.setNoticeType("教学");
        notice.setStatus(1);
        notice.setPriority(0);
        notice.setCreatedBy(creatorId);
        notice.setFeedbackCounselorId(counselorId);
        notice.setFeedbackCadreIds(cadreIds);
        notice.setPublishTime(time);
        notice.setCreatedAt(time);
        notice.setUpdatedAt(time);
        noticeMapper.insert(notice);
    }

    private void insertMessage(Long id, Long userId, Long noticeId, LocalDateTime time) {
        UserMessage message = new UserMessage();
        message.setId(id);
        message.setUserId(userId);
        message.setNoticeId(noticeId);
        message.setTitle("反馈测试通知" + noticeId);
        message.setSummary("用于反馈测试");
        message.setReadStatus(0);
        message.setCreatedAt(time);
        userMessageMapper.insert(message);
    }
}
