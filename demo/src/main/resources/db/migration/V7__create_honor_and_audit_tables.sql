-- V7__create_honor_and_audit_tables.sql
-- 审计日志相关表

-- ==========================================
-- 1. 审计日志表
-- ==========================================
CREATE TABLE audit_log (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    module VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    description VARCHAR(500),
    request_method VARCHAR(10),
    request_url VARCHAR(500),
    ip_address VARCHAR(64),
    user_agent VARCHAR(500),
    execution_time BIGINT,
    status SMALLINT NOT NULL DEFAULT 1,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE audit_log IS '审计日志表';
COMMENT ON COLUMN audit_log.module IS '操作模块：user/knowledge/party/notice';
COMMENT ON COLUMN audit_log.action IS '操作类型：create/update/delete/login/logout';
COMMENT ON COLUMN audit_log.request_method IS '请求方法：GET/POST/PUT/DELETE';
COMMENT ON COLUMN audit_log.request_url IS '请求 URL';
COMMENT ON COLUMN audit_log.ip_address IS 'IP 地址';
COMMENT ON COLUMN audit_log.user_agent IS '用户代理';
COMMENT ON COLUMN audit_log.execution_time IS '执行时间（毫秒）';
COMMENT ON COLUMN audit_log.status IS '状态：1-成功，0-失败';
COMMENT ON COLUMN audit_log.error_message IS '错误信息';

-- ==========================================
-- 4. 索引优化
-- ==========================================
CREATE INDEX idx_honor_category ON honor_record(category_id);
CREATE INDEX idx_honor_student ON honor_record(student_id);
CREATE INDEX idx_honor_status ON honor_record(status);
CREATE INDEX idx_honor_award_date ON honor_record(award_date DESC);
CREATE INDEX idx_audit_user ON audit_log(user_id);
CREATE INDEX idx_audit_module ON audit_log(module);
CREATE INDEX idx_audit_created ON audit_log(created_at DESC);
