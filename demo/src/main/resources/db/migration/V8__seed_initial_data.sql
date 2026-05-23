-- V8__seed_initial_data.sql
-- 初始化基础数据
-- 包括：角色、党团阶段、知识分类、申请类型等

-- ==========================================
-- 1. 插入系统角色
-- ==========================================
INSERT INTO t_role (id, role_code, role_name, description, status) VALUES
(1, 'student', '学生', '普通学生角色', 1),
(2, 'counselor', '辅导员', '辅导员/班主任角色', 1),
(3, 'admin', '管理员', '系统管理员角色', 1);

-- ==========================================
-- 2. 插入党团阶段定义
-- ==========================================
INSERT INTO party_stage_def (id, stage_code, stage_name, sort_order, description, status) VALUES
(1, 'applicant', '入党申请人', 1, '已提交入党申请书', 1),
(2, 'activist', '积极分子', 2, '进入积极分子培养阶段', 1),
(3, 'development_target', '发展对象', 3, '完成培训考察，进入发展对象阶段', 1),
(4, 'probationary_member', '预备党员', 4, '成为预备党员，继续考察', 1),
(5, 'full_member', '正式党员', 5, '转正完成，成为正式党员', 1);

-- ==========================================
-- 3. 插入知识分类
-- ==========================================
INSERT INTO knowledge_category (id, name, code, sort_order, status) VALUES
(1, '党团流程', 'party_process', 1, 1),
(2, '学籍事务', 'student_status', 2, 1),
(3, '奖助资助', 'scholarship_aid', 3, 1),
(4, '日常服务', 'daily_service', 4, 1);

-- ==========================================
-- 4. 插入测试用户（可选，方便开发调试）
-- ==========================================
-- 注意：实际生产环境应该删除或禁用这些测试数据
INSERT INTO t_user (id, student_no, real_name, phone, email, status) VALUES
(1001, '2023001', '张同学', '13800138001', 'zhang@example.com', 1),
(1002, '2023002', '李同学', '13800138002', 'li@example.com', 1),
(1003, '2023003', '王同学', '13800138003', 'wang@example.com', 1);

-- 为测试用户分配学生角色
INSERT INTO t_user_role (id, user_id, role_id) VALUES
(1, 1001, 1),
(2, 1002, 1),
(3, 1003, 1);

-- 插入测试用户的学生档案
INSERT INTO student_profile (id, user_id, student_no, gender, grade, major, class_name, political_status) VALUES
(1, 1001, '2023001', 1, '2023', '计算机科学与技术', '2023级1班', '积极分子'),
(2, 1002, '2023002', 2, '2023', '软件工程', '2023级2班', '共青团员'),
(3, 1003, '2023003', 1, '2023', '信息安全', '2023级3班', '发展对象');

-- 插入测试用户的党团进度
INSERT INTO party_student_progress (id, user_id, current_stage_code) VALUES
(1, 1001, 'activist'),
(2, 1002, 'applicant'),
(3, 1003, 'development_target');
