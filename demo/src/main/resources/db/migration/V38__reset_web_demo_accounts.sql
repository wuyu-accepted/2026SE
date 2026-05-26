-- V38__reset_web_demo_accounts.sql
-- Stabilize web demo accounts.

-- Admin account: admin / admin123
UPDATE t_user
SET student_no = 'admin',
    real_name = '系统管理员',
    account_type = 'admin',
    password_hash = 'PLACEHOLDER',
    status = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 2001;

-- Counselor web login requires a numeric work number.
-- Stable demo account: 10000001 / counselor123
UPDATE t_user
SET student_no = '10000001',
    real_name = '王辅导员',
    account_type = 'counselor',
    password_hash = 'PLACEHOLDER',
    status = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 2002;

DELETE FROM t_user_role
WHERE user_id IN (2001, 2002)
  AND role_id IN (
      SELECT id FROM t_role WHERE role_code IN ('student', 'cadre')
  );

INSERT INTO t_user_role (id, user_id, role_id)
SELECT 380001, 2001, r.id
FROM t_role r
WHERE r.role_code = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM t_user_role ur
      WHERE ur.user_id = 2001 AND ur.role_id = r.id
  )
  AND NOT EXISTS (
      SELECT 1 FROM t_user_role ur
      WHERE ur.id = 380001
  );

INSERT INTO t_user_role (id, user_id, role_id)
SELECT 380002, 2002, r.id
FROM t_role r
WHERE r.role_code = 'counselor'
  AND NOT EXISTS (
      SELECT 1 FROM t_user_role ur
      WHERE ur.user_id = 2002 AND ur.role_id = r.id
  )
  AND NOT EXISTS (
      SELECT 1 FROM t_user_role ur
      WHERE ur.id = 380002
  );
