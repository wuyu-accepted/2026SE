-- V9__seed_demo_content.sql
-- 为本地 H2 / PostgreSQL / Kingbase 演示补充首页、知识库与党团页面所需数据

-- ==========================================
-- 1. 通知与消息
-- ==========================================
INSERT INTO notice (id, title, summary, content, notice_type, tag, status, publish_time, priority, created_by, target_tags)
VALUES
    (10001, '学院综合服务平台上线试运行', '可在首页查看通知、待办和知识库入口。', '学院综合服务平台已进入试运行阶段，欢迎同学们体验并反馈建议。', '生活', '平台公告', 1, CURRENT_TIMESTAMP, 1, 1001, '学生'),
    (10002, '本周提交思想汇报提醒', '积极分子同学请按时提交本周思想汇报。', '请于本周内完成思想汇报提交，如有问题可在知识库查看填写说明。', '党团', '提醒', 1, CURRENT_TIMESTAMP, 2, 1001, '学生');

INSERT INTO user_message (id, user_id, notice_id, title, summary, read_status, read_time)
VALUES
    (10001, 1001, 10001, '学院综合服务平台上线试运行', '可在首页查看通知、待办和知识库入口。', 0, NULL),
    (10002, 1001, 10002, '本周提交思想汇报提醒', '积极分子同学请按时提交本周思想汇报。', 0, NULL);

-- ==========================================
-- 2. 知识库文章与模板
-- ==========================================
INSERT INTO knowledge_article (id, category_id, title, summary, content, answer, source, status, publish_time, view_count, created_by)
VALUES
    (20001, 1, '入党申请到积极分子培养流程', '梳理从提交申请书到进入积极分子培养阶段的关键步骤。', '第一步提交入党申请书，第二步参加党课培训，第三步完成谈话与考察，第四步进入积极分子培养。', '先提交申请材料，再按照学院通知参加培训与考察。', '学院党支部整理', 1, CURRENT_TIMESTAMP, 18, 1001),
    (20002, 4, '在校证明办理指引', '说明线上提交申请、辅导员审核和结果领取流程。', '进入服务平台后填写申请原因并上传必要材料，提交后等待辅导员审核。', '在线填写申请并等待审核，审核通过后按通知领取。', '学生事务中心', 1, CURRENT_TIMESTAMP, 9, 1001);

INSERT INTO knowledge_template (id, name, description, category, file_id, format, download_count, status)
VALUES
    (30001, '思想汇报模板', '适用于积极分子阶段的思想汇报模板。', '党团流程', NULL, 'DOCX', 12, 1);

-- ==========================================
-- 3. 党团记录与提醒
-- ==========================================
INSERT INTO party_student_record (id, user_id, stage_code, record_type, title, description, event_time, status)
VALUES
    (40001, 1001, 'applicant', 'submit', '提交入党申请书', '已完成入党申请书提交，并进入初步审核阶段。', CURRENT_TIMESTAMP, 1),
    (40002, 1001, 'activist', 'training', '完成积极分子第一次培训', '已完成第一阶段培训并通过课后考核。', CURRENT_TIMESTAMP, 1);

INSERT INTO party_reminder (id, user_id, title, content, deadline, status, reminder_type)
VALUES
    (50001, 1001, '提交本月思想汇报', '请在截止前完成本月思想汇报撰写并提交。', CURRENT_TIMESTAMP, 0, 'report'),
    (50002, 1001, '完善个人培养材料', '请补充近期学习与志愿活动材料。', CURRENT_TIMESTAMP, 0, 'material');
