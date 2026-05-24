-- V34__merge_honor_into_student_profile.sql
-- 将荣誉奖励与学生画像合并：支持按学期归档、绑定到用户、可选上传佐证材料

ALTER TABLE honor ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE honor ADD COLUMN IF NOT EXISTS term VARCHAR(32);
ALTER TABLE honor ADD COLUMN IF NOT EXISTS evidence_file_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_honor_user_id ON honor(user_id);
CREATE INDEX IF NOT EXISTS idx_honor_user_term ON honor(user_id, term);
