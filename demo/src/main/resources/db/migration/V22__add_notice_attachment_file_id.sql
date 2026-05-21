-- V22__add_notice_attachment_file_id.sql
-- 为通知增加可选附件文件引用，复用 file_metadata 与 /api/files/{id}/download。

ALTER TABLE notice
    ADD COLUMN attachment_file_id BIGINT;

COMMENT ON COLUMN notice.attachment_file_id IS '通知附件文件 ID，关联 file_metadata.id';
