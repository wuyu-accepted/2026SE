package com.ruc.platform.admin.notice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruc.platform.admin.notice.dto.NoticeCreateDTO;
import com.ruc.platform.admin.notice.dto.NoticeTargetDTO;
import com.ruc.platform.admin.notice.vo.NoticeDetailVO;
import com.ruc.platform.notice.entity.Notice;
import com.ruc.platform.notice.mapper.NoticeMapper;
import com.ruc.platform.notice.mapper.UserMessageMapper;
import com.ruc.platform.student.mapper.StudentProfileMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminNoticeServiceImplTest {

    @Test
    void createNoticePersistsCadresWithoutFinalFeedbackOwnerUntilPublish() {
        NoticeMapper noticeMapper = mock(NoticeMapper.class);
        UserMessageMapper userMessageMapper = mock(UserMessageMapper.class);
        when(userMessageMapper.countByNoticeId(any())).thenReturn(0L);
        AdminNoticeServiceImpl service = new AdminNoticeServiceImpl(noticeMapper, userMessageMapper, null, new ObjectMapper(), null);
        NoticeCreateDTO dto = new NoticeCreateDTO();
        dto.setTitle("反馈通知");
        dto.setContent("可反馈疑问。");
        dto.setFeedbackCadreIds(List.of(2002L, 2003L));

        NoticeDetailVO detail = service.createNotice(100L, dto);

        org.mockito.ArgumentCaptor<Notice> noticeCaptor = org.mockito.ArgumentCaptor.forClass(Notice.class);
        verify(noticeMapper).insert(noticeCaptor.capture());
        assertThat(noticeCaptor.getValue().getFeedbackCounselorId()).isNull();
        assertThat(noticeCaptor.getValue().getFeedbackCadreIds()).isEqualTo("[2002,2003]");
        assertThat(detail.getFeedbackCounselorId()).isNull();
        assertThat(detail.getFeedbackCadreIds()).containsExactly(2002L, 2003L);
    }

    @Test
    void publishNoticeSetsFinalFeedbackOwnerToPublisher() {
        NoticeMapper noticeMapper = mock(NoticeMapper.class);
        UserMessageMapper userMessageMapper = mock(UserMessageMapper.class);
        StudentProfileMapper studentProfileMapper = mock(StudentProfileMapper.class);
        Notice notice = new Notice();
        notice.setId(91001L);
        notice.setTitle("发布通知");
        notice.setContent("谁发布谁负责。");
        notice.setStatus(0);
        when(noticeMapper.selectById(91001L)).thenReturn(notice);
        when(userMessageMapper.countByNoticeId(91001L)).thenReturn(0L);
        doReturn(List.of(1001L)).when(studentProfileMapper)
                .selectTargetStudentUserIds(any(), any(), nullable(String.class), nullable(String.class));
        AdminNoticeServiceImpl service = new AdminNoticeServiceImpl(noticeMapper, userMessageMapper, studentProfileMapper, new ObjectMapper(), null);

        service.publishNotice(91001L, 2002L);

        org.mockito.ArgumentCaptor<Notice> noticeCaptor = org.mockito.ArgumentCaptor.forClass(Notice.class);
        verify(noticeMapper).updateById(noticeCaptor.capture());
        assertThat(noticeCaptor.getValue().getFeedbackCounselorId()).isEqualTo(2002L);
        assertThat(noticeCaptor.getValue().getStatus()).isEqualTo(1);
    }

    @Test
    void createNoticePersistsAndReturnsAttachmentFileId() {
        NoticeMapper noticeMapper = mock(NoticeMapper.class);
        UserMessageMapper userMessageMapper = mock(UserMessageMapper.class);
        when(userMessageMapper.countByNoticeId(any())).thenReturn(0L);
        AdminNoticeServiceImpl service = new AdminNoticeServiceImpl(noticeMapper, userMessageMapper, null, new ObjectMapper(), null);
        NoticeCreateDTO dto = new NoticeCreateDTO();
        dto.setTitle("附件通知");
        dto.setSummary("带附件");
        dto.setContent("请下载附件查看材料。");
        dto.setAttachmentFileId(99001L);

        NoticeDetailVO detail = service.createNotice(100L, dto);

        org.mockito.ArgumentCaptor<Notice> noticeCaptor = org.mockito.ArgumentCaptor.forClass(Notice.class);
        verify(noticeMapper).insert(noticeCaptor.capture());
        assertThat(noticeCaptor.getValue().getAttachmentFileId()).isEqualTo(99001L);
        assertThat(detail.getAttachmentFileId()).isEqualTo(99001L);
    }

    @Test
    void normalizeTargetSplitsPastedGradeText() {
        AdminNoticeServiceImpl service = new AdminNoticeServiceImpl(null, null, null, new ObjectMapper(), null);
        NoticeTargetDTO target = new NoticeTargetDTO();
        target.setGrades(List.of("2024本 2023本 2022硕"));

        NoticeTargetDTO normalized = ReflectionTestUtils.invokeMethod(service, "normalizeTarget", target);

        assertThat(normalized.getGrades()).containsExactly("2024本", "2023本", "2022硕");
        assertThat(normalized.getGrade()).isEqualTo("2024本");
    }
}
