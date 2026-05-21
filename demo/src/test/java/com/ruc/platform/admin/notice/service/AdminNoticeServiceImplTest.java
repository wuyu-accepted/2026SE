package com.ruc.platform.admin.notice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruc.platform.admin.notice.dto.NoticeTargetDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdminNoticeServiceImplTest {

    @Test
    void normalizeTargetSplitsPastedGradeText() {
        AdminNoticeServiceImpl service = new AdminNoticeServiceImpl(null, null, null, new ObjectMapper());
        NoticeTargetDTO target = new NoticeTargetDTO();
        target.setGrades(List.of("2024本 2023本 2022硕"));

        NoticeTargetDTO normalized = ReflectionTestUtils.invokeMethod(service, "normalizeTarget", target);

        assertThat(normalized.getGrades()).containsExactly("2024本", "2023本", "2022硕");
        assertThat(normalized.getGrade()).isEqualTo("2024本");
    }
}
