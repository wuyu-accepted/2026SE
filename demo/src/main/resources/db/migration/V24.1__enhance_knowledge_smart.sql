-- V24__enhance_knowledge_smart.sql
-- 智能知识库增强：内容元数据、行为事件与推荐日志

ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS content_type VARCHAR(32) NOT NULL DEFAULT 'guide';
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS file_id BIGINT;
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS content_mode VARCHAR(32) NOT NULL DEFAULT 'file';
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS editor_type VARCHAR(32);
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS source_content TEXT;
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS tags VARCHAR(500);
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS target_grades VARCHAR(255);
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS target_majors VARCHAR(500);
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS target_political_statuses VARCHAR(255);
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS target_party_stages VARCHAR(255);
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS scenario_codes VARCHAR(255);
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS priority INT NOT NULL DEFAULT 0;
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS effective_from TIMESTAMP;
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS effective_to TIMESTAMP;
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS updated_by BIGINT;

ALTER TABLE knowledge_template ADD COLUMN IF NOT EXISTS tags VARCHAR(500);
ALTER TABLE knowledge_template ADD COLUMN IF NOT EXISTS target_grades VARCHAR(255);
ALTER TABLE knowledge_template ADD COLUMN IF NOT EXISTS target_majors VARCHAR(500);
ALTER TABLE knowledge_template ADD COLUMN IF NOT EXISTS target_political_statuses VARCHAR(255);
ALTER TABLE knowledge_template ADD COLUMN IF NOT EXISTS target_party_stages VARCHAR(255);
ALTER TABLE knowledge_template ADD COLUMN IF NOT EXISTS scenario_codes VARCHAR(255);
ALTER TABLE knowledge_template ADD COLUMN IF NOT EXISTS priority INT NOT NULL DEFAULT 0;
ALTER TABLE knowledge_template ADD COLUMN IF NOT EXISTS effective_from TIMESTAMP;
ALTER TABLE knowledge_template ADD COLUMN IF NOT EXISTS effective_to TIMESTAMP;
ALTER TABLE knowledge_template ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE knowledge_template ADD COLUMN IF NOT EXISTS updated_by BIGINT;

CREATE TABLE IF NOT EXISTS knowledge_behavior_event (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    target_type VARCHAR(32),
    target_id BIGINT,
    keyword VARCHAR(255),
    source_page VARCHAR(64),
    feature_snapshot TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE knowledge_behavior_event IS '知识库学生行为事件表';
COMMENT ON COLUMN knowledge_behavior_event.event_type IS '行为类型：view_article/search/download_template/click_recommendation';
COMMENT ON COLUMN knowledge_behavior_event.feature_snapshot IS '触发行为时的画像和上下文快照 JSON';

CREATE TABLE IF NOT EXISTS knowledge_recommendation_log (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    score INT NOT NULL DEFAULT 0,
    reason VARCHAR(1000),
    strategy_version VARCHAR(32) NOT NULL DEFAULT 'rule-v1',
    feature_snapshot TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE knowledge_recommendation_log IS '知识库推荐结果日志表';
COMMENT ON COLUMN knowledge_recommendation_log.reason IS '推荐原因';
COMMENT ON COLUMN knowledge_recommendation_log.strategy_version IS '推荐策略版本';

CREATE INDEX IF NOT EXISTS idx_knowledge_article_content_type ON knowledge_article(content_type);
CREATE INDEX IF NOT EXISTS idx_knowledge_article_priority ON knowledge_article(priority DESC);
CREATE INDEX IF NOT EXISTS idx_knowledge_template_priority ON knowledge_template(priority DESC);
CREATE INDEX IF NOT EXISTS idx_knowledge_behavior_user_time ON knowledge_behavior_event(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_knowledge_recommend_user_time ON knowledge_recommendation_log(user_id, created_at DESC);
