-- V26__knowledge_intelligence_tasks.sql
-- 智能知识库本地开源能力：异步索引、多附件、版本治理、收藏

ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS review_status VARCHAR(32) NOT NULL DEFAULT 'approved';
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS version_no INT NOT NULL DEFAULT 1;
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS duplicate_group_id BIGINT;
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS last_index_task_id BIGINT;

CREATE TABLE IF NOT EXISTS knowledge_article_attachment (
    id BIGINT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    attachment_type VARCHAR(32) NOT NULL DEFAULT 'primary',
    sort_order INT NOT NULL DEFAULT 0,
    extracted_text TEXT,
    extract_status VARCHAR(32),
    extract_error VARCHAR(500),
    extracted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge_article_version (
    id BIGINT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    snapshot_json TEXT,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge_index_task (
    id BIGINT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    trigger_type VARCHAR(32),
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP,
    last_error VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge_favorite (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_knowledge_index_task_status ON knowledge_index_task(status, next_retry_at, created_at);
CREATE INDEX IF NOT EXISTS idx_knowledge_attachment_article ON knowledge_article_attachment(article_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_favorite_user ON knowledge_favorite(user_id, target_type, target_id);
