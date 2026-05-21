-- V25__knowledge_file_text_index.sql
-- 知识库上传文件全文抽取索引

ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS extracted_text TEXT;
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS extracted_at TIMESTAMP;
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS extract_status VARCHAR(32);
ALTER TABLE knowledge_article ADD COLUMN IF NOT EXISTS extract_error VARCHAR(500);

COMMENT ON COLUMN knowledge_article.extracted_text IS '上传 PDF/Word/TXT 文件抽取出的可检索文本';
COMMENT ON COLUMN knowledge_article.extracted_at IS '文件文字抽取时间';
COMMENT ON COLUMN knowledge_article.extract_status IS '抽取状态：success/unsupported/empty/failed/editor';
COMMENT ON COLUMN knowledge_article.extract_error IS '抽取失败或不支持原因';
