-- 本地智能知识库完整闭环：OCR 校正、检索解释、同义词、推荐权重、治理统计

ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS ocr_text TEXT;
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS ocr_corrected_text TEXT;
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS ocr_status VARCHAR(32);
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS ocr_error VARCHAR(1000);
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS ocr_corrected_by BIGINT;
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS ocr_corrected_at TIMESTAMP;
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS quality_score DECIMAL(5,2) NOT NULL DEFAULT 0;
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS expire_remind_at TIMESTAMP;
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS duplicate_signature VARCHAR(128);
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS applicable_scope VARCHAR(500);
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS reference_article_ids VARCHAR(500);

ALTER TABLE knowledge_index_task ADD COLUMN IF NOT EXISTS task_log TEXT;
ALTER TABLE knowledge_index_task ADD COLUMN IF NOT EXISTS ocr_used BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE knowledge_index_task ADD COLUMN IF NOT EXISTS duration_ms BIGINT;

CREATE TABLE IF NOT EXISTS knowledge_synonym_group (
    id BIGINT PRIMARY KEY,
    group_name VARCHAR(100) NOT NULL,
    terms TEXT NOT NULL,
    status SMALLINT NOT NULL DEFAULT 1,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge_recommend_weight_config (
    id BIGINT PRIMARY KEY,
    config_name VARCHAR(100) NOT NULL,
    ab_group VARCHAR(32) NOT NULL DEFAULT 'default',
    profile_weight DECIMAL(8,2) NOT NULL DEFAULT 1.00,
    scenario_weight DECIMAL(8,2) NOT NULL DEFAULT 1.00,
    behavior_weight DECIMAL(8,2) NOT NULL DEFAULT 1.00,
    favorite_weight DECIMAL(8,2) NOT NULL DEFAULT 1.00,
    download_weight DECIMAL(8,2) NOT NULL DEFAULT 1.00,
    success_weight DECIMAL(8,2) NOT NULL DEFAULT 1.00,
    similar_student_weight DECIMAL(8,2) NOT NULL DEFAULT 1.00,
    time_decay_weight DECIMAL(8,2) NOT NULL DEFAULT 1.00,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge_quality_feedback (
    id BIGINT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    user_id BIGINT,
    feedback_type VARCHAR(32) NOT NULL,
    score INT,
    comment VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge_ab_test_event (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    ab_group VARCHAR(32) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    target_type VARCHAR(32),
    target_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_knowledge_synonym_status ON knowledge_synonym_group(status);
CREATE INDEX IF NOT EXISTS idx_knowledge_feedback_article ON knowledge_quality_feedback(article_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_knowledge_ab_event_group ON knowledge_ab_test_event(ab_group, event_type, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_knowledge_article_expire ON knowledge_article(effective_to, status);
CREATE INDEX IF NOT EXISTS idx_knowledge_article_duplicate ON knowledge_article(duplicate_signature);
