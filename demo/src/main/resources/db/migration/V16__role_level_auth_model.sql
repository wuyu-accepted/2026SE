-- V16__role_level_auth_model.sql
-- 完善四级账号权限模型：学生、学生骨干、辅导员、超级管理员

-- 1. 用户账号类型快照，权限判断仍以 t_user_role 为准
ALTER TABLE t_user
    ADD COLUMN account_type VARCHAR(32) NOT NULL DEFAULT 'student';

COMMENT ON COLUMN t_user.account_type IS '账号类型：student-学生，cadre-学生骨干，counselor-辅导员，admin-超级管理员';

-- 2. 学生骨干作为真实角色参与鉴权
INSERT INTO t_role (id, role_code, role_name, description, status)
SELECT 4, 'cadre', '学生骨干', '班团骨干/学生骨干角色，拥有学生端增强权限', 1
WHERE NOT EXISTS (SELECT 1 FROM t_role WHERE role_code = 'cadre');

-- 3. 迁移已有账号类型
UPDATE t_user
SET account_type = 'admin'
WHERE id IN (
    SELECT ur.user_id
    FROM t_user_role ur
    INNER JOIN t_role r ON r.id = ur.role_id
    WHERE r.role_code = 'admin'
);

UPDATE t_user
SET account_type = 'counselor'
WHERE id IN (
    SELECT ur.user_id
    FROM t_user_role ur
    INNER JOIN t_role r ON r.id = ur.role_id
    WHERE r.role_code = 'counselor'
);

UPDATE t_user
SET account_type = 'cadre'
WHERE id IN (
    SELECT user_id
    FROM student_profile
    WHERE auth_type = 'cadre'
)
AND account_type = 'student';

-- 4. 为已有学生骨干补充 cadre 角色，同时保留 student 角色
INSERT INTO t_user_role (id, user_id, role_id)
SELECT 100000 + sp.user_id, sp.user_id, r.id
FROM student_profile sp
INNER JOIN t_role r ON r.role_code = 'cadre'
WHERE sp.auth_type = 'cadre'
  AND NOT EXISTS (
      SELECT 1
      FROM t_user_role ur
      WHERE ur.user_id = sp.user_id
        AND ur.role_id = r.id
  );

-- 5. 管理员和辅导员不是学生，清理历史测试迁移里误建的 student_profile
DELETE FROM student_profile
WHERE user_id IN (
    SELECT ur.user_id
    FROM t_user_role ur
    INNER JOIN t_role r ON r.id = ur.role_id
    WHERE r.role_code IN ('admin', 'counselor')
);
