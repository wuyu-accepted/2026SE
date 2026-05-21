-- V23__create_notice_feedback_tables.sql
-- 通知疑问反馈工作流：普通问题由骨干处理，私密/上报问题由辅导员处理。

ALTER TABLE notice ADD COLUMN feedback_counselor_id BIGINT;
ALTER TABLE notice ADD COLUMN feedback_cadre_ids TEXT;

COMMENT ON COLUMN notice.feedback_counselor_id IS '通知反馈最终处理辅导员 ID，默认创建人';
COMMENT ON COLUMN notice.feedback_cadre_ids IS '普通问题处理学生骨干用户 ID JSON 数组';

CREATE TABLE notice_feedback (
    id BIGINT PRIMARY KEY,
    notice_id BIGINT NOT NULL,
    message_id BIGINT,
    student_user_id BIGINT NOT NULL,
    feedback_type VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    assigned_counselor_id BIGINT NOT NULL,
    assigned_cadre_ids TEXT,
    current_handler_id BIGINT,
    handled_by BIGINT,
    handled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notice_feedback_notice FOREIGN KEY (notice_id) REFERENCES notice(id),
    CONSTRAINT fk_notice_feedback_message FOREIGN KEY (message_id) REFERENCES user_message(id)
);

COMMENT ON TABLE notice_feedback IS '通知疑问反馈表';
COMMENT ON COLUMN notice_feedback.feedback_type IS '反馈类型：ordinary-普通问题，private-私密问题';
COMMENT ON COLUMN notice_feedback.status IS '状态：pending_cadre/pending_counselor/resolved_by_cadre/resolved_by_counselor/closed';
COMMENT ON COLUMN notice_feedback.assigned_cadre_ids IS '提交时普通问题处理骨干用户 ID 快照 JSON 数组';

CREATE TABLE notice_feedback_message (
    id BIGINT PRIMARY KEY,
    feedback_id BIGINT NOT NULL,
    sender_user_id BIGINT NOT NULL,
    sender_role VARCHAR(32) NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    content TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notice_feedback_message_feedback FOREIGN KEY (feedback_id) REFERENCES notice_feedback(id)
);

COMMENT ON TABLE notice_feedback_message IS '通知疑问反馈处理日志表';
COMMENT ON COLUMN notice_feedback_message.action_type IS '动作类型：submit/cadre_reply/escalate/counselor_reply/resolve';

CREATE INDEX idx_notice_feedback_notice ON notice_feedback(notice_id);
CREATE INDEX idx_notice_feedback_student ON notice_feedback(student_user_id, created_at DESC);
CREATE INDEX idx_notice_feedback_counselor ON notice_feedback(assigned_counselor_id, status, updated_at DESC);
CREATE INDEX idx_notice_feedback_status ON notice_feedback(status, updated_at DESC);
CREATE INDEX idx_notice_feedback_message_feedback ON notice_feedback_message(feedback_id, created_at ASC);
