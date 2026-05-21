package com.ruc.platform.notice.mapper;

import com.ruc.platform.PlatformApplication;
import com.ruc.platform.notice.vo.MessageDetailVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PlatformApplication.class)
@ActiveProfiles("h2")
class UserMessageMapperTest {

    @Autowired
    private UserMessageMapper userMessageMapper;

    @Test
    void selectsMessageDetailByNoticeIdForCurrentUser() {
        MessageDetailVO detail = userMessageMapper.selectDetailByNoticeIdAndUserId(10001L, 1001L);

        assertThat(detail).isNotNull();
        assertThat(detail.getNoticeId()).isEqualTo(10001L);
        assertThat(detail.getTitle()).isNotBlank();
        assertThat(detail.getContent()).isNotBlank();
    }
}
