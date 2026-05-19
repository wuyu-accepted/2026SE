-- 为历史学生账号补齐缺失的 student_profile，避免合并或旧数据导致学生端查询报错

INSERT INTO student_profile (id, user_id, student_no, auth_type, created_at, updated_at)
SELECT
    COALESCE((SELECT MAX(existing.id) FROM student_profile existing), 0) + ROW_NUMBER() OVER (ORDER BY u.id),
    u.id,
    u.student_no,
    CASE
        WHEN u.account_type = 'cadre' THEN 'cadre'
        ELSE 'student'
    END,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM t_user u
WHERE u.account_type IN ('student', 'cadre')
  AND NOT EXISTS (
      SELECT 1
      FROM student_profile sp
      WHERE sp.user_id = u.id
  );
