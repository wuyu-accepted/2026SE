-- V24__student_import_demo_users.sql
-- 学生账号改为管理端导入，并补充 stu1/stu2/stu3 测试账号。

-- 清理历史测试迁移里给管理员/辅导员误补的学生骨干角色，管理员固定为 admin，辅导员固定为 counselor。
DELETE FROM t_user_role
WHERE user_id IN (
    SELECT id FROM t_user WHERE account_type IN ('admin', 'counselor')
)
AND role_id IN (
    SELECT id FROM t_role WHERE role_code IN ('student', 'cadre')
);

-- stu1 / stu2 普通学生，stu3 学生骨干。默认密码由 AdminAccountInitializer 写成 password。
INSERT INTO t_user (id, student_no, real_name, password_hash, account_type, phone, email, status, created_at, updated_at)
SELECT 3001, 'stu1', '测试学生1', 'PLACEHOLDER', 'student', '13900000001', 'stu1@example.com', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM t_user WHERE student_no = 'stu1');

INSERT INTO t_user (id, student_no, real_name, password_hash, account_type, phone, email, status, created_at, updated_at)
SELECT 3002, 'stu2', '测试学生2', 'PLACEHOLDER', 'student', '13900000002', 'stu2@example.com', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM t_user WHERE student_no = 'stu2');

INSERT INTO t_user (id, student_no, real_name, password_hash, account_type, phone, email, status, created_at, updated_at)
SELECT 3003, 'stu3', '测试骨干3', 'PLACEHOLDER', 'cadre', '13900000003', 'stu3@example.com', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM t_user WHERE student_no = 'stu3');

INSERT INTO t_user_role (id, user_id, role_id)
SELECT 3001, 3001, id FROM t_role
WHERE role_code = 'student'
  AND NOT EXISTS (SELECT 1 FROM t_user_role WHERE user_id = 3001 AND role_id = t_role.id);

INSERT INTO t_user_role (id, user_id, role_id)
SELECT 3002, 3002, id FROM t_role
WHERE role_code = 'student'
  AND NOT EXISTS (SELECT 1 FROM t_user_role WHERE user_id = 3002 AND role_id = t_role.id);

INSERT INTO t_user_role (id, user_id, role_id)
SELECT 3003, 3003, id FROM t_role
WHERE role_code = 'student'
  AND NOT EXISTS (SELECT 1 FROM t_user_role WHERE user_id = 3003 AND role_id = t_role.id);

INSERT INTO t_user_role (id, user_id, role_id)
SELECT 3004, 3003, id FROM t_role
WHERE role_code = 'cadre'
  AND NOT EXISTS (SELECT 1 FROM t_user_role WHERE user_id = 3003 AND role_id = t_role.id);

INSERT INTO student_profile (
    id, user_id, student_no, gender, grade, major, class_name, political_status, auth_type, created_at, updated_at
)
SELECT 3001, 3001, 'stu1', 1, '2023本', '计算机科学与技术', '2023级1班', '共青团员', 'student', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM student_profile WHERE user_id = 3001);

INSERT INTO student_profile (
    id, user_id, student_no, gender, grade, major, class_name, political_status, auth_type, created_at, updated_at
)
SELECT 3002, 3002, 'stu2', 2, '2022硕', '软件工程', '2022级硕士班', '共青团员', 'student', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM student_profile WHERE user_id = 3002);

INSERT INTO student_profile (
    id, user_id, student_no, gender, grade, major, class_name, political_status, auth_type, created_at, updated_at
)
SELECT 3003, 3003, 'stu3', 1, '2021博', '信息安全', '2021级博士班', '中共党员', 'cadre', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM student_profile WHERE user_id = 3003);
