-- V31__cleanup_demo_accounts.sql
-- 统一测试账号体系

-- 清理旧的角色分配（去重）
DELETE FROM t_user_role WHERE id IN (
    SELECT id FROM (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY user_id, role_id ORDER BY id) AS rn
        FROM t_user_role
    ) t WHERE t.rn > 1
);

-- 更新学生账号学号和名字，设置统一密码
UPDATE t_user SET
    student_no = '2023001',
    real_name = '张同学',
    password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
WHERE id = 1001;

UPDATE t_user SET
    student_no = '2023002',
    real_name = '李同学',
    password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
WHERE id = 1002;

UPDATE t_user SET
    student_no = '2023003',
    real_name = '王同学（骨干）',
    password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
WHERE id = 1003;

-- 给王同学（1003）分配 cadre 角色
MERGE INTO t_user_role (id, user_id, role_id) KEY(id)
SELECT 9003, 1003, (SELECT id FROM t_role WHERE role_code = 'cadre');

-- 更新 stu1/stu2/stu3 学号为可读格式
UPDATE t_user SET
    student_no = '2023004',
    real_name = '测试学生A（普通）',
    password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
WHERE id = 3001;

UPDATE t_user SET
    student_no = '2023005',
    real_name = '测试学生B（普通）',
    password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
WHERE id = 3002;

UPDATE t_user SET
    student_no = '2023006',
    real_name = '测试学生C（骨干）',
    password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
WHERE id = 3003;

-- 更新管理员学号
UPDATE t_user SET student_no = 'admin' WHERE id = 2001;

-- 更新辅导员学号
UPDATE t_user SET student_no = 'counselor1' WHERE id = 2002;

-- 更新 student_profile 中的学号
UPDATE student_profile SET student_no = '2023001' WHERE user_id = 1001;
UPDATE student_profile SET student_no = '2023002' WHERE user_id = 1002;
UPDATE student_profile SET student_no = '2023003' WHERE user_id = 1003;
UPDATE student_profile SET student_no = '2023004' WHERE user_id = 3001;
UPDATE student_profile SET student_no = '2023005' WHERE user_id = 3002;
UPDATE student_profile SET student_no = '2023006' WHERE user_id = 3003;

-- 更新 user_message 关联（之前的 user_message 关联到了旧的 3001/3002，无变化）
-- 给 1001（张同学）加消息
MERGE INTO user_message (id, user_id, notice_id, title, summary, read_status, created_at) KEY(id)
SELECT 80001, 1001, n.id, n.title, n.summary, 0, CURRENT_TIMESTAMP FROM notice n
WHERE n.status = 1 AND n.id IN (30001, 30002, 30003);

-- 给 1003（王同学/骨干）加消息
MERGE INTO user_message (id, user_id, notice_id, title, summary, read_status, created_at) KEY(id)
SELECT 80010, 1003, n.id, n.title, n.summary, 0, CURRENT_TIMESTAMP FROM notice n
WHERE n.status = 1 AND n.id IN (9001, 9002, 30001, 30002);
