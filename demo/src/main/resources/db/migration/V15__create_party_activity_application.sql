CREATE TABLE party_activity_application (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    event_date DATE,
    reviewer_id BIGINT,
    status SMALLINT NOT NULL DEFAULT 0,
    submit_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    review_comment VARCHAR(500),
    reviewed_by BIGINT,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_party_activity_user FOREIGN KEY (user_id) REFERENCES t_user(id),
    CONSTRAINT fk_party_activity_reviewer FOREIGN KEY (reviewer_id) REFERENCES t_user(id),
    CONSTRAINT fk_party_activity_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES t_user(id)
);

CREATE INDEX idx_party_activity_user ON party_activity_application(user_id, submit_time DESC);
CREATE INDEX idx_party_activity_status ON party_activity_application(status, submit_time DESC);
