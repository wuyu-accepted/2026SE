-- V25__numeric_demo_account_ids.sql
-- 将本地测试账号的登录学号/工号统一调整为数字，姓名字段保留原测试名。

UPDATE t_user
SET student_no = '10000001',
    real_name = 'counselor',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 2002
  AND account_type = 'counselor';

UPDATE t_user
SET student_no = '00000001',
    real_name = 'stu1',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 3001
  AND account_type = 'student';

UPDATE student_profile
SET student_no = '00000001',
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = 3001;

UPDATE t_user
SET student_no = '00000002',
    real_name = 'stu2',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 3002
  AND account_type = 'student';

UPDATE student_profile
SET student_no = '00000002',
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = 3002;

UPDATE t_user
SET student_no = '00000003',
    real_name = 'stu3',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 3003
  AND account_type = 'cadre';

UPDATE student_profile
SET student_no = '00000003',
    updated_at = CURRENT_TIMESTAMP
WHERE user_id = 3003;
