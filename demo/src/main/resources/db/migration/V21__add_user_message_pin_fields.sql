ALTER TABLE user_message ADD COLUMN pinned_status SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE user_message ADD COLUMN pinned_time TIMESTAMP;

COMMENT ON COLUMN user_message.pinned_status IS '置顶状态：0-未置顶，1-已置顶';
COMMENT ON COLUMN user_message.pinned_time IS '置顶时间';

CREATE INDEX idx_message_pinned ON user_message(user_id, pinned_status, pinned_time DESC);
