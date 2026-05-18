-- V14__seed_counselor_admin_users.sql
-- 为辅导员/管理员端提供可登录的演示账号（仅用于开发环境）

INSERT INTO t_user (id, student_no, real_name, phone, email, status, password_hash)
VALUES
    (2001, '9000001', '演示辅导员', '13800000001', 'counselor@example.com', 1, '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'),
    (2002, '9000002', '演示管理员', '13800000002', 'admin@example.com', 1, '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy');

INSERT INTO t_user_role (id, user_id, role_id)
VALUES
    (20001, 2001, 2),
    (20002, 2002, 3);
