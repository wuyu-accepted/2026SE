package com.ruc.platform.common.util;

import com.ruc.platform.common.api.ResultCode;
import com.ruc.platform.common.exception.BizException;

import java.util.regex.Pattern;

public final class GradeUtils {

    private static final Pattern GRADE_PATTERN = Pattern.compile("^\\d{4}[本硕博]$");
    private static final String ERROR_MESSAGE = "年级格式应为四位年份+本/硕/博，例如：2023本、2022硕、2023博";

    private GradeUtils() {
    }

    public static String normalizeRequiredGrade(String grade) {
        String normalized = clean(grade);
        if (normalized == null || !GRADE_PATTERN.matcher(normalized).matches()) {
            throw new BizException(ResultCode.PARAM_ERROR, ERROR_MESSAGE);
        }
        return normalized;
    }

    public static String normalizeOptionalGrade(String grade) {
        String normalized = clean(grade);
        if (normalized == null) {
            return null;
        }
        if (!GRADE_PATTERN.matcher(normalized).matches()) {
            throw new BizException(ResultCode.PARAM_ERROR, ERROR_MESSAGE);
        }
        return normalized;
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
