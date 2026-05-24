CREATE TABLE IF NOT EXISTS study_course_record (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    course_name VARCHAR(255) NOT NULL,
    category VARCHAR(64) NOT NULL,
    credits DECIMAL(6,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_study_course_record_user_id ON study_course_record(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_study_course_record_user_course_category ON study_course_record(user_id, course_name, category);
