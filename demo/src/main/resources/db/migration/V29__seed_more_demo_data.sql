-- V29__seed_more_demo_data.sql
-- 为所有已有用户补充消息、党团进度和请假等演示数据

-- 为 stu1/stu2/stu3 创建通知消息
INSERT INTO user_message (id, user_id, notice_id, title, summary, read_status)
SELECT
    (SELECT COALESCE(MAX(um.id), 100000) FROM user_message um) + ROW_NUMBER() OVER (),
    u.id,
    n.id,
    n.title,
    n.summary,
    0
FROM t_user u
CROSS JOIN notice n
WHERE n.status = 1
  AND u.id IN (3001, 3002, 3003)
  AND NOT EXISTS (
      SELECT 1 FROM user_message um2
      WHERE um2.user_id = u.id AND um2.notice_id = n.id
  );
