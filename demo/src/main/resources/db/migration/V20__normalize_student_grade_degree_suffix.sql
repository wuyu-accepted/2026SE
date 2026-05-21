-- Require student_profile.grade to use 四位年份 + 本/硕/博.
-- Existing demo data only stored the year; default historical year-only rows to 本科.

UPDATE student_profile
SET grade = grade || '本',
    updated_at = CURRENT_TIMESTAMP
WHERE grade ~ '^\d{4}$';

COMMENT ON COLUMN student_profile.grade IS '年级：四位年份+本/硕/博，例如 2023本、2022硕、2023博';
