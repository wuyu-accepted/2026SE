-- 首页轮播图和用户自定义快捷入口

-- 1. 知识条目增加是否设为首页轮播标记
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS is_banner BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. 通知公告增加是否设为首页轮播标记
ALTER TABLE notice ADD COLUMN IF NOT EXISTS is_banner BOOLEAN NOT NULL DEFAULT FALSE;

-- 3. 首页轮播图表
CREATE TABLE IF NOT EXISTS home_banner (
    id BIGINT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    subtitle VARCHAR(500),
    image_url VARCHAR(500),
    target_type VARCHAR(32) NOT NULL DEFAULT 'none',
    target_id BIGINT,
    target_path VARCHAR(200),
    source_type VARCHAR(32) NOT NULL DEFAULT 'manual',
    source_article_id BIGINT,
    source_notice_id BIGINT,
    sort_order INT NOT NULL DEFAULT 0,
    status SMALLINT NOT NULL DEFAULT 1,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. 用户自定义快捷入口表
CREATE TABLE IF NOT EXISTS user_quick_entry (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    entry_code VARCHAR(64) NOT NULL,
    entry_name VARCHAR(100) NOT NULL,
    entry_icon VARCHAR(32) NOT NULL DEFAULT '',
    entry_path VARCHAR(200) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_home_banner_status ON home_banner(status, sort_order);
CREATE INDEX IF NOT EXISTS idx_user_quick_entry_user ON user_quick_entry(user_id, sort_order);
