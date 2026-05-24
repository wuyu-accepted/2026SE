-- V30__seed_full_mock_data.sql
-- 全链路 mock 数据（使用已知用户 ID）

-- ==========================================
-- 1. 管理员与辅导员角色分配
-- ==========================================
INSERT INTO t_user_role (id, user_id, role_id)
VALUES (8001, 2001, (SELECT id FROM t_role WHERE role_code = 'admin'))
ON CONFLICT (id) DO NOTHING;

INSERT INTO t_user_role (id, user_id, role_id)
VALUES (8002, 2002, (SELECT id FROM t_role WHERE role_code = 'counselor'))
ON CONFLICT (id) DO NOTHING;


-- 给 stu1/stu2/stu3 设置密码
UPDATE t_user SET password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy' WHERE id IN (3001, 3002, 3003);

-- ==========================================
-- 2. 设置轮播通知
-- ==========================================
UPDATE notice SET is_banner = true WHERE id IN (9001, 10001, 10002);

-- ==========================================
-- 3. 通知公告
-- ==========================================
INSERT INTO notice (id, title, summary, content, notice_type, tag, status, publish_time, priority, is_banner, created_by, target_tags)
VALUES (30001, '2026年秋季学期选课通知', '请同学们在规定时间内完成选课操作。', '各位同学：2026年秋季学期选课将于6月1日开始，至6月15日结束。', '教学', '选课', 1, CURRENT_TIMESTAMP, 1, true, 2002, '全体学生')
ON CONFLICT (id) DO NOTHING;

INSERT INTO notice (id, title, summary, content, notice_type, tag, status, publish_time, priority, is_banner, created_by, target_tags)
VALUES (30002, '暑期社会实践通知', '鼓励同学们积极参与暑期社会实践活动。', '活动主题涵盖乡村振兴、红色教育、社区服务等方向。', '生活', '实践', 1, CURRENT_TIMESTAMP, 0, true, 2002, '全体学生')
ON CONFLICT (id) DO NOTHING;

INSERT INTO notice (id, title, summary, content, notice_type, tag, status, publish_time, priority, is_banner, created_by, target_tags)
VALUES (30003, '心理健康讲座', '校心理健康中心举办专题讲座。', '时间：5月30日 14:00，地点：学生活动中心报告厅。', '生活', '心理', 1, CURRENT_TIMESTAMP, 0, false, 2002, '全体学生')
ON CONFLICT (id) DO NOTHING;

-- ==========================================
-- 4. 知识库分类
-- ==========================================
INSERT INTO knowledge_category (id, name, code, sort_order, status)
VALUES (100, '奖助学金', 'scholarship', 5, 1)
ON CONFLICT (id) DO NOTHING;
INSERT INTO knowledge_category (id, name, code, sort_order, status)
VALUES (101, '就业创业', 'employment', 6, 1)
ON CONFLICT (id) DO NOTHING;

-- ==========================================
-- 5. 知识库文章
-- ==========================================
INSERT INTO knowledge_article (id, category_id, title, summary, content, content_type, status, publish_time, view_count, is_banner, created_by, tags)
VALUES (40001, 1, '入党申请书撰写规范', '格式要求、内容要点和提交流程。', '入党申请书是向党组织表达入党意愿的正式文书。格式要求：标题居中写"入党申请书"。', 'guide', 1, CURRENT_TIMESTAMP, 56, true, 2002, '入党,申请书,流程')
ON CONFLICT (id) DO NOTHING;

INSERT INTO knowledge_article (id, category_id, title, summary, content, content_type, status, publish_time, view_count, is_banner, created_by, tags)
VALUES (40002, 100, '国家奖学金申请指南', '申请条件、评审标准和材料准备全流程。', '国家奖学金奖励标准：8000元/人。申请条件：学习成绩排名前10%。', 'guide', 1, CURRENT_TIMESTAMP, 128, true, 2002, '奖学金,资助,申请')
ON CONFLICT (id) DO NOTHING;

INSERT INTO knowledge_article (id, category_id, title, summary, content, content_type, status, publish_time, view_count, is_banner, created_by, tags)
VALUES (40003, 4, '学生证补办流程', '遗失或损坏后的补办步骤和所需材料。', '1. 填写补办申请 2. 提交一寸蓝底照片 3. 缴纳工本费 4. 3个工作日后领取。', 'process', 1, CURRENT_TIMESTAMP, 89, false, 2002, '学生证,补办,证件')
ON CONFLICT (id) DO NOTHING;

INSERT INTO knowledge_article (id, category_id, title, summary, content, content_type, status, publish_time, view_count, is_banner, created_by, tags)
VALUES (40004, 101, '简历撰写与面试技巧', '求职简历撰写要点和面试策略。', '简历要点：一页为佳、STAR法则、量化成果。面试准备：了解企业背景、1分钟自我介绍。', 'guide', 1, CURRENT_TIMESTAMP, 67, false, 2002, '就业,简历,面试')
ON CONFLICT (id) DO NOTHING;

-- ==========================================
-- 6. 知识模板
-- ==========================================
INSERT INTO knowledge_template (id, name, description, category, format, download_count, status)
VALUES (50001, '思想汇报模板（详细版）', '适用于发展对象阶段的思想汇报模板。', '党团流程', 'DOCX', 42, 1)
ON CONFLICT (id) DO NOTHING;
INSERT INTO knowledge_template (id, name, description, category, format, download_count, status)
VALUES (50002, '请假申请表（标准版）', '学院标准请假申请表。', '日常服务', 'DOCX', 36, 1)
ON CONFLICT (id) DO NOTHING;

-- ==========================================
-- 7. 首页轮播图
-- ==========================================
INSERT INTO home_banner (id, title, subtitle, target_type, target_id, source_type, sort_order, status, created_by)
VALUES (60001, '国家奖学金开始申报', '奖励优秀学生，每人8000元', 'knowledge', 40002, 'manual', 1, 1, 2002)
ON CONFLICT (id) DO NOTHING;
INSERT INTO home_banner (id, title, subtitle, target_type, target_id, source_type, sort_order, status, created_by)
VALUES (60002, '入党申请书撰写规范', '了解入党流程第一步', 'knowledge', 40001, 'manual', 2, 1, 2002)
ON CONFLICT (id) DO NOTHING;
INSERT INTO home_banner (id, title, subtitle, target_type, target_id, source_type, sort_order, status, created_by)
VALUES (60003, '暑期社会实践报名中', '乡村振兴·红色教育·社区服务', 'notice', 30002, 'manual', 3, 1, 2002)
ON CONFLICT (id) DO NOTHING;

-- ==========================================
-- 8. 给用户发消息
-- ==========================================
INSERT INTO user_message (id, user_id, notice_id, title, summary, read_status, created_at)
VALUES (70001, 3001, 30001, '2026年秋季学期选课通知', '请同学们在规定时间内完成选课操作。', 0, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
INSERT INTO user_message (id, user_id, notice_id, title, summary, read_status, created_at)
VALUES (70002, 3001, 30002, '暑期社会实践通知', '鼓励同学们积极参与。', 0, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
INSERT INTO user_message (id, user_id, notice_id, title, summary, read_status, created_at)
VALUES (70003, 3001, 30003, '心理健康讲座', '校心理健康中心举办专题讲座。', 0, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
INSERT INTO user_message (id, user_id, notice_id, title, summary, read_status, created_at)
VALUES (70004, 3001, 9001, '关于2026春季学期学生事务办理安排', '请同学们按时间节点办理事务。', 0, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
INSERT INTO user_message (id, user_id, notice_id, title, summary, read_status, created_at)
VALUES (70005, 3001, 9002, '党团积极分子材料提交提醒', '请本月内完成思想汇报上传。', 0, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
INSERT INTO user_message (id, user_id, notice_id, title, summary, read_status, created_at)
VALUES (70006, 3002, 30001, '2026年秋季学期选课通知', '请同学们在规定时间内完成选课操作。', 0, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;
