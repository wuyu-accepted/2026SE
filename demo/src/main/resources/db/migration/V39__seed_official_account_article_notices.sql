-- V39__seed_official_account_article_notices.sql
-- 插入学校/学院官方公众号文章作为通知，投递至所有学生
--
-- 这两条通知的 target_tags 设为空数组 JSON，表示全体学生可见。
-- user_message 通过动态 SQL 为所有状态正常的 student/cadre 用户生成消息记录，
-- 确保新注册的学生也能通过 autoSeedMessagesIfEmpty 自动获取。

-- ==========================================
-- 1. 插入公众号文章通知
-- ==========================================
INSERT INTO notice (id, title, summary, content, notice_type, tag, status, publish_time, priority, is_banner, created_by, target_tags)
SELECT 50001,
       '学术速递丨中国人民大学信息学院师生论文被数据挖掘领域顶会KDD 2026录用',
       '中国人民大学信息学院师生论文被数据挖掘领域顶级会议 KDD 2026 录用，展现学院在数据科学前沿研究的强劲实力。',
       'https://mp.weixin.qq.com/s/RI0F2-3-kxrWWcfx4_vW7w',
       '教学', '学术动态', 1, CURRENT_TIMESTAMP, 1, false, 2002,
       '{"grades":[],"majors":[],"className":null,"authType":null,"grade":null,"major":null}'
WHERE NOT EXISTS (SELECT 1 FROM notice WHERE id = 50001);

INSERT INTO notice (id, title, summary, content, notice_type, tag, status, publish_time, priority, is_banner, created_by, target_tags)
SELECT 50002,
       '热血加冕 不负热爱丨信息学院男篮斩获校男子篮球联赛乙组冠军',
       '信息学院男子篮球队奋勇拼搏，成功斩获校男子篮球联赛乙组冠军，展现了信息学子的青春风采与团队精神。',
       'https://mp.weixin.qq.com/s/ytAzxBIv1M71pQ0Kt19dpQ',
       '生活', '文体活动', 1, CURRENT_TIMESTAMP, 0, false, 2002,
       '{"grades":[],"majors":[],"className":null,"authType":null,"grade":null,"major":null}'
WHERE NOT EXISTS (SELECT 1 FROM notice WHERE id = 50002);

-- ==========================================
-- 2. 为所有学生投递消息
-- ==========================================
INSERT INTO user_message (id, user_id, notice_id, title, summary, read_status, created_at)
SELECT
    (SELECT COALESCE(MAX(um.id), 100000) FROM user_message um) + ROW_NUMBER() OVER (ORDER BY u.id, n.id),
    u.id,
    n.id,
    n.title,
    n.summary,
    0,
    CURRENT_TIMESTAMP
FROM t_user u
CROSS JOIN (SELECT id, title, summary FROM notice WHERE id IN (50001, 50002)) n
INNER JOIN student_profile sp ON sp.user_id = u.id
WHERE u.status = 1
  AND sp.auth_type IN ('student', 'cadre')
  AND NOT EXISTS (
      SELECT 1 FROM user_message um2
      WHERE um2.user_id = u.id AND um2.notice_id = n.id
  );
