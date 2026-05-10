-- V12__seed_home_demo_data.sql
-- 首页演示数据初始化
-- 包括：已发布通知、知识模板（用于首页 downloads 展示）

-- ==========================================
-- 1. 首页通知（已发布）
-- ==========================================
INSERT INTO notice (
    id, title, summary, content, notice_type, tag, status, publish_time, priority, created_by, target_tags
) VALUES
(9001, '关于 2026 春季学期学生事务办理时间安排的通知',
 '请同学们按时间节点办理学籍异动、缓考与证明申请等事务。',
 '各位同学：为保障春季学期事务办理有序进行，请按学院公布时间节点提交材料，逾期原则上不再补办。',
 '教学', '事务办理', 1, '2026-04-10 09:00:00', 1, 3, '2023级,全体学生'),
(9002, '党团积极分子阶段材料提交提醒',
 '请本月内完成思想汇报与培训记录上传。',
 '党团流程提醒：积极分子阶段同学需在本月最后一个工作日前提交思想汇报并补齐培训记录。',
 '党团', '阶段提醒', 1, '2026-04-14 10:30:00', 2, 2, '积极分子,2023级'),
(9003, '学院服务平台功能更新说明',
 '首页、知识库、党团进度模块已完成升级。',
 '本次更新优化了首页数据聚合能力，新增知识模板展示，并改善党团进度信息可读性。',
 '其他', '系统公告', 1, '2026-04-16 18:00:00', 0, 3, '全体学生');

-- ==========================================
-- 2. 演示文件元数据
-- ==========================================
INSERT INTO file_metadata (
    id, biz_type, origin_name, stored_name, storage_path, mime_type, file_size, uploader_id, status
) VALUES
(9201, 'template', 'thought-report-template.txt', 'thought-report-template.txt',
 'mock-files/thought-report-template.txt', 'text/plain', 256, 3, 1),
(9202, 'template', 'student-info-fix-template.txt', 'student-info-fix-template.txt',
 'mock-files/student-info-fix-template.txt', 'text/plain', 256, 3, 1),
(9203, 'template', 'aid-application-guide.txt', 'aid-application-guide.txt',
 'mock-files/aid-application-guide.txt', 'text/plain', 256, 3, 1),
(9204, 'template', 'practice-certificate-template.txt', 'practice-certificate-template.txt',
 'mock-files/practice-certificate-template.txt', 'text/plain', 256, 3, 1);

-- ==========================================
-- 3. 首页下载模板（启用）
-- ==========================================
INSERT INTO knowledge_template (
    id, name, description, category, file_id, format, download_count, status
) VALUES
(9101, '思想汇报模板（通用版）', '适用于积极分子与发展对象阶段的思想汇报撰写。', '党团流程', 9201, 'TXT', 24, 1),
(9102, '学籍信息更正申请表', '用于个人基本信息或学籍字段更正申请。', '学籍事务', 9202, 'TXT', 12, 1),
(9103, '困难补助申请说明与模板', '包含申请条件、材料清单与填写示例。', '奖助资助', 9203, 'TXT', 36, 1),
(9104, '社会实践证明模板', '校内外社会实践活动证明模板。', '日常服务', 9204, 'TXT', 8, 1);
