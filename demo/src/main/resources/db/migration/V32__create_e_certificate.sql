-- V32__create_e_certificate.sql
-- 电子证明申请表（独立于 certificate_application）

CREATE TABLE e_certificate (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    reason VARCHAR(1000),
    template_type VARCHAR(64) NOT NULL DEFAULT 'general',
    status SMALLINT NOT NULL DEFAULT 0,
    submit_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_by BIGINT,
    approved_at TIMESTAMP,
    reject_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE e_certificate IS '电子证明申请表';
COMMENT ON COLUMN e_certificate.id IS '主键ID';
COMMENT ON COLUMN e_certificate.user_id IS '申请人用户ID';
COMMENT ON COLUMN e_certificate.title IS '证明标题';
COMMENT ON COLUMN e_certificate.reason IS '申请理由';
COMMENT ON COLUMN e_certificate.template_type IS '模板类型：general-通用, enrollment-在读证明, scholarship-奖学金证明, graduation-毕业证明';
COMMENT ON COLUMN e_certificate.status IS '状态：0-待审批, 2-已通过, 3-已驳回';
COMMENT ON COLUMN e_certificate.submit_time IS '提交时间';
COMMENT ON COLUMN e_certificate.approved_by IS '审批人用户ID';
COMMENT ON COLUMN e_certificate.approved_at IS '审批时间';
COMMENT ON COLUMN e_certificate.reject_reason IS '驳回原因';
COMMENT ON COLUMN e_certificate.created_at IS '创建时间';
COMMENT ON COLUMN e_certificate.updated_at IS '更新时间';

CREATE INDEX idx_e_certificate_user_id ON e_certificate(user_id);
CREATE INDEX idx_e_certificate_status ON e_certificate(status);
