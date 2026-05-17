-- V14__seed_admin_counselor_accounts.sql
-- 为管理端（Vue 网页）添加管理员和辅导员测试账号
-- 管理员：学号 admin，密码 admin123
-- 辅导员：学号 counselor，密码 counselor123

-- 使用 Spring Boot 启动后会自动通过 PasswordEncoder 更新这些默认密码，
-- 但为了首次 Flyway 迁移能通过，这里先用占位哈希
-- 应用首次启动后，AdminAccountInitializer 会将密码更新为正确的 BCrypt 哈希

-- ==========================================
-- 1. 管理员账号（role_id = 3）
-- ==========================================
INSERT INTO t_user (id, student_no, real_name, password_hash, phone, email, status, created_at, updated_at)
SELECT 2001, 'admin', '系统管理员', 'PLACEHOLDER', '13800000001', 'admin@ruc.edu.cn', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM t_user WHERE id = 2001);

INSERT INTO t_user_role (id, user_id, role_id)
SELECT 100, 2001, 3
WHERE NOT EXISTS (SELECT 1 FROM t_user_role WHERE user_id = 2001 AND role_id = 3);

INSERT INTO student_profile (id, user_id, student_no, auth_type, grade, major, class_name, created_at, updated_at)
SELECT 100, 2001, 'admin', 'cadre', '2020', '计算机科学与技术', '教职工', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM student_profile WHERE user_id = 2001);

-- ==========================================
-- 2. 辅导员账号（role_id = 2）
-- ==========================================
INSERT INTO t_user (id, student_no, real_name, password_hash, phone, email, status, created_at, updated_at)
SELECT 2002, 'counselor', '王辅导员', 'PLACEHOLDER', '13800000002', 'counselor@ruc.edu.cn', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM t_user WHERE id = 2002);

INSERT INTO t_user_role (id, user_id, role_id)
SELECT 101, 2002, 2
WHERE NOT EXISTS (SELECT 1 FROM t_user_role WHERE user_id = 2002 AND role_id = 2);

INSERT INTO student_profile (id, user_id, student_no, auth_type, grade, major, class_name, created_at, updated_at)
SELECT 101, 2002, 'counselor', 'cadre', '2020', '思想政治教育', '教职工', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM student_profile WHERE user_id = 2002);
