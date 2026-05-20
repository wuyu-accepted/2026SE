-- Restore seeded demo account 2023001 as a student account.
-- V10 temporarily gave it counselor privileges for early local approval testing,
-- but V17 then treated it as non-student and removed its student_profile,
-- which prevents notice delivery to the commonly used mini-program demo login.

DELETE FROM t_user_role
WHERE user_id = 1001
  AND role_id IN (SELECT id FROM t_role WHERE role_code = 'counselor');

UPDATE t_user
SET account_type = 'student',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 1001;

INSERT INTO student_profile (
    id,
    user_id,
    student_no,
    gender,
    grade,
    major,
    class_name,
    political_status,
    auth_type,
    created_at,
    updated_at
)
SELECT
    COALESCE((SELECT MAX(existing.id) FROM student_profile existing), 0) + 1,
    1001,
    '2023001',
    1,
    '2023',
    '计算机科学与技术',
    '2023级1班',
    '积极分子',
    'student',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM student_profile WHERE user_id = 1001
);

UPDATE student_profile
SET auth_type = 'student',
    grade = COALESCE(NULLIF(grade, ''), '2023'),
    major = COALESCE(NULLIF(major, ''), '计算机科学与技术'),
    class_name = COALESCE(NULLIF(class_name, ''), '2023级1班'),
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = 1001;
