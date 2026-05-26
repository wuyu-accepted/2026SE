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
SELECT 1001, '2023001', '张同学', 'PLACEHOLDER', 'student', '13800138001', 'zhang@example.com', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM t_user WHERE id = 1001);

INSERT INTO t_user (id, student_no, real_name, password_hash, account_type, phone, email, status, created_at, updated_at)
SELECT 1002, '2023002', '李同学', 'PLACEHOLDER', 'student', '13800138002', 'li@example.com', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM t_user WHERE id = 1002);

INSERT INTO t_user (id, student_no, real_name, password_hash, account_type, phone, email, status, created_at, updated_at)
SELECT 1003, '2023003', '王同学（骨干）', 'PLACEHOLDER', 'cadre', '13800138003', 'wang@example.com', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM t_user WHERE id = 1003);

INSERT INTO t_user (id, student_no, real_name, password_hash, account_type, phone, email, status, created_at, updated_at)
SELECT 3001, '2023004', '测试学生A（普通）', 'PLACEHOLDER', 'student', '13900000001', 'stu1@example.com', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM t_user WHERE id = 3001);

INSERT INTO t_user (id, student_no, real_name, password_hash, account_type, phone, email, status, created_at, updated_at)
SELECT 3002, '2023005', '测试学生B（普通）', 'PLACEHOLDER', 'student', '13900000002', 'stu2@example.com', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM t_user WHERE id = 3002);

INSERT INTO t_user (id, student_no, real_name, password_hash, account_type, phone, email, status, created_at, updated_at)
SELECT 3003, '2023006', '测试学生C（骨干）', 'PLACEHOLDER', 'cadre', '13900000003', 'stu3@example.com', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM t_user WHERE id = 3003);

-- Normalize login numbers, names, account types, and password.
UPDATE t_user
SET student_no = '2023001',
    real_name = '张同学',
    account_type = 'student',
    password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    status = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 1001;

UPDATE t_user
SET student_no = '2023002',
    real_name = '李同学',
    account_type = 'student',
    password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    status = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 1002;

UPDATE t_user
SET student_no = '2023003',
    real_name = '王同学（骨干）',
    account_type = 'cadre',
    password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    status = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 1003;

UPDATE t_user
SET student_no = '2023004',
    real_name = '测试学生A（普通）',
    account_type = 'student',
    password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    status = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 3001;

UPDATE t_user
SET student_no = '2023005',
    real_name = '测试学生B（普通）',
    account_type = 'student',
    password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    status = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 3002;

UPDATE t_user
SET student_no = '2023006',
    real_name = '测试学生C（骨干）',
    account_type = 'cadre',
    password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    status = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 3003;

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
SELECT 370201, 1001, '2023001', 1, '2023', '计算机科学与技术', '2023级1班', '积极分子', 'student', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM student_profile WHERE user_id = 1001);

INSERT INTO student_profile (
    id, user_id, student_no, gender, grade, major, class_name, political_status, auth_type, created_at, updated_at
)
SELECT 370202, 1002, '2023002', 2, '2023', '软件工程', '2023级2班', '共青团员', 'student', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM student_profile WHERE user_id = 1002);

INSERT INTO student_profile (
    id, user_id, student_no, gender, grade, major, class_name, political_status, auth_type, created_at, updated_at
)
SELECT 370203, 1003, '2023003', 1, '2023', '信息安全', '2023级3班', '发展对象', 'cadre', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM student_profile WHERE user_id = 1003);

INSERT INTO student_profile (
    id, user_id, student_no, gender, grade, major, class_name, political_status, auth_type, created_at, updated_at
)
SELECT 370204, 3001, '2023004', 1, '2022', '计算机科学与技术', '2022级1班', '共青团员', 'student', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM student_profile WHERE user_id = 3001);

INSERT INTO student_profile (
    id, user_id, student_no, gender, grade, major, class_name, political_status, auth_type, created_at, updated_at
)
SELECT 370205, 3002, '2023005', 2, '2022', '软件工程', '2022级2班', '群众', 'student', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM student_profile WHERE user_id = 3002);

INSERT INTO student_profile (
    id, user_id, student_no, gender, grade, major, class_name, political_status, auth_type, created_at, updated_at
)
SELECT 370206, 3003, '2023006', 1, '2021博', '信息安全', '2021级博士班', '中共党员', 'cadre', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM student_profile WHERE user_id = 3003);

UPDATE student_profile
SET student_no = '2023001',
    gender = 1,
    grade = '2023',
    major = '计算机科学与技术',
    class_name = '2023级1班',
    political_status = '积极分子',
    auth_type = 'student',
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = 1001;

UPDATE student_profile
SET student_no = '2023002',
    gender = 2,
    grade = '2023',
    major = '软件工程',
    class_name = '2023级2班',
    political_status = '共青团员',
    auth_type = 'student',
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = 1002;

UPDATE student_profile
SET student_no = '2023003',
    gender = 1,
    grade = '2023',
    major = '信息安全',
    class_name = '2023级3班',
    political_status = '发展对象',
    auth_type = 'cadre',
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = 1003;

UPDATE student_profile
SET student_no = '2023004',
    gender = 1,
    grade = '2022',
    major = '计算机科学与技术',
    class_name = '2022级1班',
    political_status = '共青团员',
    auth_type = 'student',
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = 3001;

UPDATE student_profile
SET student_no = '2023005',
    gender = 2,
    grade = '2022',
    major = '软件工程',
    class_name = '2022级2班',
    political_status = '群众',
    auth_type = 'student',
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = 3002;

UPDATE student_profile
SET student_no = '2023006',
    gender = 1,
    grade = '2021博',
    major = '信息安全',
    class_name = '2021级博士班',
    political_status = '中共党员',
    auth_type = 'cadre',
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = 3003;

-- Ensure party progress exists so the party progress page can render.
INSERT INTO party_student_progress (id, user_id, current_stage_code, current_step_code, updated_at)
SELECT 370301, 1001, 'activist', 'activist_periodic_reports', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM party_student_progress WHERE user_id = 1001);

INSERT INTO party_student_progress (id, user_id, current_stage_code, current_step_code, updated_at)
SELECT 370302, 1002, 'applicant', 'applicant_submit_application', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM party_student_progress WHERE user_id = 1002);

INSERT INTO party_student_progress (id, user_id, current_stage_code, current_step_code, updated_at)
SELECT 370303, 1003, 'development_target', 'dev_target_periodic_reports', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM party_student_progress WHERE user_id = 1003);

INSERT INTO party_student_progress (id, user_id, current_stage_code, current_step_code, updated_at)
SELECT 370304, 3001, 'applicant', 'applicant_submit_application', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM party_student_progress WHERE user_id = 3001);

INSERT INTO party_student_progress (id, user_id, current_stage_code, current_step_code, updated_at)
SELECT 370305, 3002, 'activist', 'activist_periodic_reports', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM party_student_progress WHERE user_id = 3002);

INSERT INTO party_student_progress (id, user_id, current_stage_code, current_step_code, updated_at)
SELECT 370306, 3003, 'development_target', 'dev_target_periodic_reports', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM party_student_progress WHERE user_id = 3003);

UPDATE party_student_progress
SET current_stage_code = 'activist',
    current_step_code = 'activist_periodic_reports',
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = 1001;

UPDATE party_student_progress
SET current_stage_code = 'applicant',
    current_step_code = 'applicant_submit_application',
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = 1002;

UPDATE party_student_progress
SET current_stage_code = 'development_target',
    current_step_code = 'dev_target_periodic_reports',
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = 1003;

UPDATE party_student_progress
SET current_stage_code = 'applicant',
    current_step_code = 'applicant_submit_application',
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = 3001;

UPDATE party_student_progress
SET current_stage_code = 'activist',
    current_step_code = 'activist_periodic_reports',
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = 3002;

UPDATE party_student_progress
SET current_stage_code = 'development_target',
    current_step_code = 'dev_target_periodic_reports',
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = 3003;
