package com.ruc.platform.common.util;

import com.ruc.platform.common.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GradeUtilsTest {

    @Test
    void acceptsFourDigitYearWithDegreeSuffix() {
        assertThat(GradeUtils.normalizeRequiredGrade(" 2023本 ")).isEqualTo("2023本");
        assertThat(GradeUtils.normalizeRequiredGrade("2022硕")).isEqualTo("2022硕");
        assertThat(GradeUtils.normalizeRequiredGrade("2023博")).isEqualTo("2023博");
    }

    @Test
    void rejectsYearWithoutDegreeSuffix() {
        assertThatThrownBy(() -> GradeUtils.normalizeRequiredGrade("2023"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("年级格式应为四位年份+本/硕/博");
    }

    @Test
    void allowsBlankOptionalGrade() {
        assertThat(GradeUtils.normalizeOptionalGrade(" ")).isNull();
    }
}
