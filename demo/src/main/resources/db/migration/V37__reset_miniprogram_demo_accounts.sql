-- V37__reset_miniprogram_demo_accounts.sql
-- Stabilize mini-program demo accounts.
--
-- Do not delete old demo rows: many feature tables reference user_id.
-- Instead, normalize the known demo users to fixed login accounts and reset
-- their password hash to "password".

-- Ensure the cadre role exists.
INSERT INTO t_role (id, role_code, role_name, description, status)
SELECT 4, 'cadre', '学生骨干', '班团骨干/学生骨干角色，拥有学生端增强权限', 1
WHERE NOT EXISTS (SELECT 1 FROM t_role WHERE role_code = 'cadre');

-- Ensure all demo users exist.
INSERT INTO t_user (id, student_no, real_name, password_hash, account_type, phone, email, status, created_at, updated_at)
VALUES
    (1001, '2023001', '张同学', 'PLACEHOLDER', 'student', '13800138001', 'zhang@example.com', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1002, '2023002', '李同学', 'PLACEHOLDER', 'student', '13800138002', 'li@example.com', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1003, '2023003', '王同学（骨干）', 'PLACEHOLDER', 'cadre', '13800138003', 'wang@example.com', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3001, '2023004', '测试学生A（普通）', 'PLACEHOLDER', 'student', '13900000001', 'stu1@example.com', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3002, '2023005', '测试学生B（普通）', 'PLACEHOLDER', 'student', '13900000002', 'stu2@example.com', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3003, '2023006', '测试学生C（骨干）', 'PLACEHOLDER', 'cadre', '13900000003', 'stu3@example.com', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Normalize login numbers, names, account types, and password.
UPDATE t_user
SET student_no = v.student_no,
    real_name = v.real_name,
    account_type = v.account_type,
    password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    status = 1,
    updated_at = CURRENT_TIMESTAMP
FROM (
    VALUES
        (1001, '2023001', '张同学', 'student'),
        (1002, '2023002', '李同学', 'student'),
        (1003, '2023003', '王同学（骨干）', 'cadre'),
        (3001, '2023004', '测试学生A（普通）', 'student'),
        (3002, '2023005', '测试学生B（普通）', 'student'),
        (3003, '2023006', '测试学生C（骨干）', 'cadre')
) AS v(id, student_no, real_name, account_type)
WHERE t_user.id = v.id;

-- Remove admin/counselor roles from mini-program demo users if historical
-- migrations accidentally granted them.
DELETE FROM t_user_role
WHERE user_id IN (1001, 1002, 1003, 3001, 3002, 3003)
  AND role_id IN (
      SELECT id FROM t_role WHERE role_code IN ('admin', 'counselor')
  );

-- Ensure every mini-program demo user has the student role.
INSERT INTO t_user_role (id, user_id, role_id)
SELECT 370001 + row_number() OVER (ORDER BY u.id), u.id, r.id
FROM t_user u
CROSS JOIN t_role r
WHERE u.id IN (1001, 1002, 1003, 3001, 3002, 3003)
  AND r.role_code = 'student'
  AND NOT EXISTS (
      SELECT 1 FROM t_user_role ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

-- Ensure cadre demo users also have the cadre role.
INSERT INTO t_user_role (id, user_id, role_id)
SELECT 370101 + row_number() OVER (ORDER BY u.id), u.id, r.id
FROM t_user u
CROSS JOIN t_role r
WHERE u.id IN (1003, 3003)
  AND r.role_code = 'cadre'
  AND NOT EXISTS (
      SELECT 1 FROM t_user_role ur
      WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

-- Normalize student profiles.
INSERT INTO student_profile (
    id, user_id, student_no, gender, grade, major, class_name, political_status, auth_type, created_at, updated_at
)
VALUES
    (370201, 1001, '2023001', 1, '2023', '计算机科学与技术', '2023级1班', '积极分子', 'student', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (370202, 1002, '2023002', 2, '2023', '软件工程', '2023级2班', '共青团员', 'student', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (370203, 1003, '2023003', 1, '2023', '信息安全', '2023级3班', '发展对象', 'cadre', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (370204, 3001, '2023004', 1, '2022', '计算机科学与技术', '2022级1班', '共青团员', 'student', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (370205, 3002, '2023005', 2, '2022', '软件工程', '2022级2班', '群众', 'student', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (370206, 3003, '2023006', 1, '2021博', '信息安全', '2021级博士班', '中共党员', 'cadre', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (user_id) DO UPDATE
SET student_no = EXCLUDED.student_no,
    gender = EXCLUDED.gender,
    grade = EXCLUDED.grade,
    major = EXCLUDED.major,
    class_name = EXCLUDED.class_name,
    political_status = EXCLUDED.political_status,
    auth_type = EXCLUDED.auth_type,
    updated_at = CURRENT_TIMESTAMP;

-- Ensure party progress exists so the party progress page can render.
INSERT INTO party_student_progress (id, user_id, current_stage_code, current_step_code, updated_at)
VALUES
    (370301, 1001, 'activist', 'activist_periodic_reports', CURRENT_TIMESTAMP),
    (370302, 1002, 'applicant', 'applicant_submit_application', CURRENT_TIMESTAMP),
    (370303, 1003, 'development_target', 'dev_target_periodic_reports', CURRENT_TIMESTAMP),
    (370304, 3001, 'applicant', 'applicant_submit_application', CURRENT_TIMESTAMP),
    (370305, 3002, 'activist', 'activist_periodic_reports', CURRENT_TIMESTAMP),
    (370306, 3003, 'development_target', 'dev_target_periodic_reports', CURRENT_TIMESTAMP)
ON CONFLICT (user_id) DO UPDATE
SET current_stage_code = EXCLUDED.current_stage_code,
    current_step_code = EXCLUDED.current_step_code,
    updated_at = CURRENT_TIMESTAMP;
