UPDATE party_step_def
SET step_name = '定期思想汇报与教育考察培养',
    description = '按要求提交思想汇报，并持续接受教育考察培养',
    sort_order = 2
WHERE step_code = 'activist_periodic_reports';

UPDATE party_step_def
SET step_name = '支部意见与推优',
    description = '听取支部意见并完成推优',
    sort_order = 3
WHERE step_code = 'activist_branch_opinion';

UPDATE party_step_def
SET step_name = '院党校学习与结业',
    description = '进入院党校学习并完成结业',
    sort_order = 4
WHERE step_code = 'activist_college_party_school';

UPDATE party_step_def
SET status = 0
WHERE step_code = 'activist_training_and_assess';

UPDATE party_step_guidance
SET title = '按期提交思想汇报并接受培养考察',
    content = '按支部要求定期提交思想汇报，并同步完成教育考察培养记录。',
    materials = '["思想汇报（按要求提交）","培养考察记录（如有）"]'
WHERE step_code = 'activist_periodic_reports';

UPDATE party_step_guidance
SET title = '形成支部意见并完成推优',
    content = '积极分子阶段需听取支部意见，并完成推优程序。',
    materials = '["支部意见材料（如有）","推优材料（如有）"]'
WHERE step_code = 'activist_branch_opinion';

UPDATE party_step_guidance
SET title = '完成院党校学习与结业',
    content = '积极分子阶段可进入院党校学习并完成结业。',
    materials = '["院党校结业材料（如有）"]'
WHERE step_code = 'activist_college_party_school';

UPDATE party_step_guidance
SET status = 0
WHERE step_code = 'activist_training_and_assess';

UPDATE party_step_def
SET sort_order = 1
WHERE step_code = 'dev_target_confirmed';

UPDATE party_step_def
SET step_name = '定期思想汇报',
    description = '按要求定期提交思想汇报',
    sort_order = 2
WHERE step_code = 'dev_target_periodic_reports';

UPDATE party_step_def
SET step_name = '校党校学习与结业',
    description = '完成校党校学习并结业',
    sort_order = 3
WHERE step_code = 'dev_target_school_party_school';

UPDATE party_step_def
SET step_name = '支部接收与党委审批',
    description = '完成支部接收并报党委审批',
    sort_order = 4
WHERE step_code = 'dev_target_committee_approve';

UPDATE party_step_def
SET status = 0
WHERE step_code IN ('dev_target_training_and_assess', 'dev_target_branch_opinion');

UPDATE party_step_guidance
SET title = '按期提交思想汇报',
    content = '发展对象阶段需按要求定期提交思想汇报。',
    materials = '["思想汇报（按要求提交）"]'
WHERE step_code = 'dev_target_periodic_reports';

UPDATE party_step_guidance
SET title = '完成校党校学习与结业',
    content = '发展对象阶段需进入校党校学习并完成结业。',
    materials = '["校党校结业材料（如有）"]'
WHERE step_code = 'dev_target_school_party_school';

UPDATE party_step_guidance
SET title = '完成支部接收与党委审批',
    content = '发展对象阶段需完成支部接收并报党委审批。',
    materials = '["支部接收材料（如有）","党委审批材料（如有）"]'
WHERE step_code = 'dev_target_committee_approve';

UPDATE party_step_guidance
SET status = 0
WHERE step_code IN ('dev_target_training_and_assess', 'dev_target_branch_opinion');

UPDATE party_step_def
SET sort_order = 1
WHERE step_code = 'probationary_branch_meeting';

UPDATE party_step_def
SET sort_order = 2
WHERE step_code = 'probationary_oath';

UPDATE party_step_def
SET step_name = '预备期培养考察（1年）',
    description = '预备期为1年，期间持续接受培养考察',
    sort_order = 3
WHERE step_code = 'probationary_period';

UPDATE party_step_def
SET step_name = '支部大会讨论转正与党委审批',
    description = '预备期满后完成支部大会讨论转正并报党委审批',
    sort_order = 4
WHERE step_code = 'probationary_to_full_member';

UPDATE party_step_def
SET status = 0
WHERE step_code = 'probationary_reports_and_assess';

UPDATE party_step_guidance
SET title = '完成预备期培养考察',
    content = '预备期一般为1年，期间持续接受培养考察并准备转正。',
    materials = '["预备期考察记录（如有）"]'
WHERE step_code = 'probationary_period';

UPDATE party_step_guidance
SET title = '完成转正讨论与党委审批',
    content = '预备期满后，经支部大会讨论同意并报党委审批后转为正式党员。',
    materials = '["转正申请（如有）","党委审批材料（如有）"]'
WHERE step_code = 'probationary_to_full_member';

UPDATE party_step_guidance
SET status = 0
WHERE step_code = 'probationary_reports_and_assess';

UPDATE party_student_progress
SET current_step_code = 'activist_periodic_reports'
WHERE current_step_code = 'activist_training_and_assess';

UPDATE party_student_progress
SET current_step_code = 'dev_target_periodic_reports'
WHERE current_step_code IN ('dev_target_training_and_assess', 'dev_target_branch_opinion');

UPDATE party_student_progress
SET current_step_code = 'probationary_period'
WHERE current_step_code = 'probationary_reports_and_assess';
