-- V13__party_progress_tracker.sql

ALTER TABLE party_student_progress
    ADD COLUMN current_step_code VARCHAR(64);

CREATE TABLE party_step_def (
    id BIGINT PRIMARY KEY,
    stage_code VARCHAR(64) NOT NULL,
    step_code VARCHAR(64) NOT NULL UNIQUE,
    step_name VARCHAR(128) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    description VARCHAR(500),
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_party_step_stage_code FOREIGN KEY (stage_code) REFERENCES party_stage_def(stage_code)
);

CREATE INDEX idx_party_step_stage ON party_step_def(stage_code, sort_order);

CREATE TABLE party_stage_history (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    stage_code VARCHAR(64) NOT NULL,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    remark VARCHAR(500),
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_party_stage_history_user FOREIGN KEY (user_id) REFERENCES t_user(id),
    CONSTRAINT fk_party_stage_history_stage_code FOREIGN KEY (stage_code) REFERENCES party_stage_def(stage_code)
);

CREATE INDEX idx_party_stage_history_user ON party_stage_history(user_id, stage_code);

CREATE TABLE party_step_guidance (
    id BIGINT PRIMARY KEY,
    step_code VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content VARCHAR(1000),
    materials TEXT,
    priority INT NOT NULL DEFAULT 1,
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_party_step_guidance_step_title UNIQUE (step_code, title),
    CONSTRAINT fk_party_step_guidance_step_code FOREIGN KEY (step_code) REFERENCES party_step_def(step_code)
);

CREATE INDEX idx_party_step_guidance_step ON party_step_guidance(step_code, priority);

INSERT INTO party_step_def (id, stage_code, step_code, step_name, sort_order, description, status) VALUES
    (13001, 'applicant', 'applicant_submit_application', '提交入党申请书', 1, '以入党申请书落款日期为准', 1),
    (13002, 'applicant', 'applicant_study_group', '参加党课学习小组学习', 2, '可进入党课学习小组学习', 1),
    (13003, 'applicant', 'applicant_recommendation', '学习小组推荐与推优', 3, '学习小组推荐与推优', 1),

    (13101, 'activist', 'activist_confirmed', '确定为入党积极分子', 1, '进入积极分子培养阶段', 1),
    (13102, 'activist', 'activist_periodic_reports', '定期思想汇报', 2, '按要求提交思想汇报', 1),
    (13103, 'activist', 'activist_training_and_assess', '教育考察与培养记录', 3, '教育考察与培养记录', 1),
    (13104, 'activist', 'activist_branch_opinion', '支部意见与推优', 4, '形成支部意见并完成推优', 1),
    (13105, 'activist', 'activist_college_party_school', '院党校学习与结业', 5, '可进入院党校学习', 1),

    (13201, 'development_target', 'dev_target_confirmed', '确定为发展对象', 1, '进入发展对象阶段', 1),
    (13202, 'development_target', 'dev_target_periodic_reports', '定期思想汇报', 2, '按要求提交思想汇报', 1),
    (13203, 'development_target', 'dev_target_training_and_assess', '教育考察与培养记录', 3, '教育考察与培养记录', 1),
    (13204, 'development_target', 'dev_target_branch_opinion', '支部意见与培养考察', 4, '形成支部意见并完成培养考察', 1),
    (13205, 'development_target', 'dev_target_school_party_school', '校党校学习与结业', 5, '可进入校党校学习', 1),
    (13206, 'development_target', 'dev_target_committee_approve', '支部接收与党委审批', 6, '支部接收与党委审批', 1),

    (13301, 'probationary_member', 'probationary_branch_meeting', '支部大会通过为预备党员', 1, '以支部大会通过之日为入党时间', 1),
    (13302, 'probationary_member', 'probationary_oath', '入党宣誓', 2, '完成入党宣誓', 1),
    (13303, 'probationary_member', 'probationary_period', '预备期培养考察（1年）', 3, '预备期一般为1年', 1),
    (13304, 'probationary_member', 'probationary_reports_and_assess', '定期思想汇报与教育考察', 4, '按要求提交思想汇报与接受考察', 1),
    (13305, 'probationary_member', 'probationary_to_full_member', '支部大会讨论转正与党委审批', 5, '完成转正流程', 1),

    (13401, 'full_member', 'full_member_done', '成为正式党员', 1, '转正完成', 1);

INSERT INTO party_step_guidance (id, step_code, title, content, materials, priority, status) VALUES
    (13501, 'applicant_submit_application', '填写并提交入党申请书', '入党申请书落款日期作为申请入党的起算日期。', '["入党申请书（签名、日期）"]', 1, 1),
    (13502, 'applicant_study_group', '参加党课学习小组', '按要求参与学习小组学习与活动记录。', '["学习小组学习记录（如有）"]', 2, 1),
    (13503, 'applicant_recommendation', '等待推荐与推优', '完成学习小组推荐与推优流程。', '["推荐/推优材料（如有）"]', 1, 1),

    (13601, 'activist_confirmed', '进入积极分子培养阶段', '进入积极分子培养考察阶段。', '[]', 1, 1),
    (13602, 'activist_periodic_reports', '按期提交思想汇报', '按支部要求定期提交思想汇报。', '["思想汇报（按要求提交）"]', 1, 1),
    (13603, 'activist_training_and_assess', '参加教育考察与培养', '参加培训并完善培养考察记录。', '["培训签到/证明（如有）","培养考察记录（如有）"]', 2, 1),
    (13604, 'activist_branch_opinion', '形成支部意见', '形成支部意见并完成推优。', '["支部意见材料（如有）"]', 1, 1),
    (13605, 'activist_college_party_school', '参加院党校学习', '可进入院党校学习并完成结业。', '["院党校结业材料（如有）"]', 2, 1),

    (13701, 'dev_target_confirmed', '进入发展对象阶段', '进入发展对象培养考察阶段。', '[]', 1, 1),
    (13702, 'dev_target_periodic_reports', '按期提交思想汇报', '按支部要求定期提交思想汇报。', '["思想汇报（按要求提交）"]', 1, 1),
    (13703, 'dev_target_training_and_assess', '完善培养考察记录', '完善教育考察、培养记录。', '["培养考察记录（如有）"]', 2, 1),
    (13704, 'dev_target_branch_opinion', '支部意见与考察', '形成支部意见并完成培养考察。', '["支部意见材料（如有）"]', 1, 1),
    (13705, 'dev_target_school_party_school', '参加校党校学习', '可进入校党校学习并完成结业。', '["校党校结业材料（如有）"]', 2, 1),
    (13706, 'dev_target_committee_approve', '支部接收与党委审批', '完成支部接收与党委审批。', '["审批材料（如有）"]', 1, 1),

    (13801, 'probationary_branch_meeting', '确定入党时间', '支部大会通过其为预备党员之日为入党时间。', '["支部大会决议（如有）"]', 1, 1),
    (13802, 'probationary_oath', '完成入党宣誓', '按要求完成入党宣誓。', '["入党宣誓记录（如有）"]', 2, 1),
    (13803, 'probationary_period', '预备期培养考察', '预备期一般为1年，按要求完成培养考察。', '["预备期考察记录（如有）"]', 1, 1),
    (13804, 'probationary_reports_and_assess', '持续思想汇报与考察', '按要求提交思想汇报并接受教育考察。', '["思想汇报（按要求提交）","考察记录（如有）"]', 2, 1),
    (13805, 'probationary_to_full_member', '完成转正流程', '支部大会讨论转正，党委审批通过后成为正式党员。', '["转正申请（如有）","审批材料（如有）"]', 1, 1),

    (13901, 'full_member_done', '流程完成', '已成为正式党员。', '[]', 1, 1);

UPDATE party_student_progress
SET current_step_code = CASE current_stage_code
    WHEN 'applicant' THEN 'applicant_submit_application'
    WHEN 'activist' THEN 'activist_confirmed'
    WHEN 'development_target' THEN 'dev_target_confirmed'
    WHEN 'probationary_member' THEN 'probationary_branch_meeting'
    WHEN 'full_member' THEN 'full_member_done'
    ELSE NULL
END
WHERE current_step_code IS NULL;

INSERT INTO party_stage_history (id, user_id, stage_code, start_time, end_time, remark, updated_by)
VALUES
    (14001, 1002, 'applicant', '2026-03-01 09:00:00', NULL, '已提交入党申请书，等待学习小组安排。', 1001),
    (14002, 1001, 'applicant', '2026-01-10 09:00:00', '2026-02-15 18:00:00', '完成申请材料提交与学习小组学习。', 1001),
    (14003, 1001, 'activist', '2026-02-16 09:00:00', NULL, '进入积极分子培养阶段，按期提交思想汇报。', 1001),
    (14004, 1003, 'development_target', '2026-02-01 09:00:00', NULL, '进入发展对象阶段，准备校党校学习。', 1001);

