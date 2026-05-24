CREATE TABLE IF NOT EXISTS ai_provider_config (
    id BIGINT PRIMARY KEY,
    config_name VARCHAR(100) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    base_url VARCHAR(255) NOT NULL,
    api_key_cipher TEXT,
    api_key_mask VARCHAR(64),
    model VARCHAR(100) NOT NULL,
    temperature DECIMAL(4,2),
    top_p DECIMAL(4,2),
    max_tokens INT,
    presence_penalty DECIMAL(4,2),
    frequency_penalty DECIMAL(4,2),
    response_format VARCHAR(32),
    timeout_seconds INT NOT NULL DEFAULT 30,
    stream_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    retrieval_top_k INT NOT NULL DEFAULT 5,
    action_top_k INT NOT NULL DEFAULT 3,
    system_prompt TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT
);

CREATE TABLE IF NOT EXISTS ai_conversation (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200),
    client_type VARCHAR(32) NOT NULL DEFAULT 'miniprogram',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_message (
    id BIGINT PRIMARY KEY,
    conversation_id BIGINT,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    provider VARCHAR(50),
    model VARCHAR(100),
    citations_json TEXT,
    actions_json TEXT,
    prompt_tokens INT,
    completion_tokens INT,
    total_tokens INT,
    latency_ms INT,
    status VARCHAR(32) NOT NULL DEFAULT 'success',
    error_message VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_provider_active ON ai_provider_config(active);
CREATE INDEX IF NOT EXISTS idx_ai_conversation_user_time ON ai_conversation(user_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_message_conversation_time ON ai_message(conversation_id, created_at);
