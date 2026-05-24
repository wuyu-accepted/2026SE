UPDATE study_course_record
SET created_at = CURRENT_TIMESTAMP
WHERE created_at IS NULL;

UPDATE study_course_record
SET updated_at = CURRENT_TIMESTAMP
WHERE updated_at IS NULL;

ALTER TABLE study_course_record
ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE study_course_record
ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
