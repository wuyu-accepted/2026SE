-- V6__create_file_and_approval_tables.sql
-- 文件管理与审批流程相关表
-- 包括：文件元数据、证明申请、审批记录

-- ==========================================
-- 1. 文件元数据表
-- ==========================================
CREATE TABLE file_metadata (
    id BIGINT PRIMARY KEY,
    biz_type VARCHAR(32),
    origin_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100),
    file_size BIGINT,
    uploader_id BIGINT,
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE file_metadata IS '文件元数据表';
COMMENT ON COLUMN file_metadata.biz_type IS '业务类型：template/report/certificate/attachment';
COMMENT ON COLUMN file_metadata.origin_name IS '原始文件名';
COMMENT ON COLUMN file_metadata.stored_name IS '存储文件名';
COMMENT ON COLUMN file_metadata.storage_path IS '存储路径';
COMMENT ON COLUMN file_metadata.mime_type IS 'MIME 类型';
COMMENT ON COLUMN file_metadata.file_size IS '文件大小（字节）';
COMMENT ON COLUMN file_metadata.uploader_id IS '上传人 ID';
COMMENT ON COLUMN file_metadata.status IS '状态：1-正常，0-已删除';

-- ==========================================
-- 2. 申请记录表
-- ==========================================
CREATE TABLE certificate_application (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type_code VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    reason VARCHAR(1000),
    file_id BIGINT,
    status SMALLINT NOT NULL DEFAULT 0,
    submit_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_by BIGINT,
    approved_at TIMESTAMP,
    reject_reason VARCHAR(500),
    certificate_file_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_application_user FOREIGN KEY (user_id) REFERENCES t_user(id),
    CONSTRAINT fk_application_file FOREIGN KEY (file_id) REFERENCES file_metadata(id)
);

COMMENT ON TABLE certificate_application IS '证明/申请表';
COMMENT ON COLUMN certificate_application.type_code IS '申请类型编码';
COMMENT ON COLUMN certificate_application.title IS '申请标题';
COMMENT ON COLUMN certificate_application.reason IS '申请理由';
COMMENT ON COLUMN certificate_application.file_id IS '附件文件 ID';
COMMENT ON COLUMN certificate_application.status IS '状态：0-待审批，1-审批中，2-已通过，3-已驳回，4-已撤回';
COMMENT ON COLUMN certificate_application.approved_by IS '审批人 ID';
COMMENT ON COLUMN certificate_application.approved_at IS '审批时间';
COMMENT ON COLUMN certificate_application.reject_reason IS '驳回理由';
COMMENT ON COLUMN certificate_application.certificate_file_id IS '证明文件 ID';

-- ==========================================
-- 4. 审批记录表
-- ==========================================
CREATE TABLE approval_record (
    id BIGINT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    approver_id BIGINT NOT NULL,
    action VARCHAR(32) NOT NULL,
    comment VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_approval_application FOREIGN KEY (application_id) REFERENCES certificate_application(id),
    CONSTRAINT fk_approval_approver FOREIGN KEY (approver_id) REFERENCES t_user(id)
);

COMMENT ON TABLE approval_record IS '审批记录表';
COMMENT ON COLUMN approval_record.action IS '操作：submit/approve/reject/reopen/withdraw';
COMMENT ON COLUMN approval_record.comment IS '审批意见';

-- ==========================================
-- 5. 索引优化
-- ==========================================
CREATE INDEX idx_file_biz_type ON file_metadata(biz_type);
CREATE INDEX idx_file_uploader ON file_metadata(uploader_id);
CREATE INDEX idx_application_user ON certificate_application(user_id);
CREATE INDEX idx_application_status ON certificate_application(status);
CREATE INDEX idx_application_type ON certificate_application(type_code);
CREATE INDEX idx_application_submit_time ON certificate_application(submit_time DESC);
CREATE INDEX idx_approval_application ON approval_record(application_id);
