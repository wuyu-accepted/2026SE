-- V33__create_honor.sql
-- 奖励荣誉表

CREATE TABLE honor (
    id BIGINT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    student_name VARCHAR(64),
    student_no VARCHAR(32),
    award_level VARCHAR(32),
    award_date VARCHAR(32),
    description VARCHAR(500),
    category VARCHAR(64),
    status SMALLINT DEFAULT 1,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_honor_status ON honor(status);
