package com.ruc.platform.student.mapper;

import com.ruc.platform.PlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PlatformApplication.class)
@ActiveProfiles("h2")
class StudentProfileMapperTest {

    @Autowired
    private StudentProfileMapper studentProfileMapper;

    @Test
    void selectsTargetStudentUserIdsByMultipleGradesAndMajors() {
        List<Long> userIds = studentProfileMapper.selectTargetStudentUserIds(
                List.of("2023"),
                List.of("计算机科学与技术", "软件工程", "信息安全"),
                null,
                null
        );

        assertThat(userIds).contains(1001L, 1002L, 1003L);
    }

    @Test
    void selectsTargetStudentUserIdsWhenOneOfMultipleGradesMatches() {
        List<Long> userIds = studentProfileMapper.selectTargetStudentUserIds(
                List.of("2024", "2023", "2022"),
                null,
                null,
                null
        );

        assertThat(userIds).contains(1001L, 1002L, 1003L);
    }

    @Test
    void selectsTargetStudentUserIdsWhenGradesAreSplitFromPastedText() {
        List<Long> userIds = studentProfileMapper.selectTargetStudentUserIds(
                List.of("2024", "2023", "2022"),
                null,
                null,
                null
        );

        assertThat(userIds).contains(1001L, 1002L, 1003L);
    }
}
