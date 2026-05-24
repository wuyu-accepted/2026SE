-- Repair demo/legacy feedback ownership for the rule:
-- whoever publishes the notice is the final feedback owner.

UPDATE notice n
SET feedback_counselor_id = n.created_by
WHERE EXISTS (
    SELECT 1
    FROM t_user_role ur
    INNER JOIN t_role r ON r.id = ur.role_id
    WHERE ur.user_id = n.created_by
      AND r.role_code IN ('counselor', 'admin')
)
AND (
    n.feedback_counselor_id IS NULL
    OR n.feedback_counselor_id <> n.created_by
);

UPDATE notice n
SET created_by = (
        SELECT u.id
        FROM t_user u
        INNER JOIN t_user_role ur ON ur.user_id = u.id
        INNER JOIN t_role r ON r.id = ur.role_id
        WHERE r.role_code = 'counselor'
          AND u.status = 1
        ORDER BY u.id ASC
        LIMIT 1
    ),
    feedback_counselor_id = (
        SELECT u.id
        FROM t_user u
        INNER JOIN t_user_role ur ON ur.user_id = u.id
        INNER JOIN t_role r ON r.id = ur.role_id
        WHERE r.role_code = 'counselor'
          AND u.status = 1
        ORDER BY u.id ASC
        LIMIT 1
    )
WHERE NOT EXISTS (
    SELECT 1
    FROM t_user_role ur
    INNER JOIN t_role r ON r.id = ur.role_id
    WHERE ur.user_id = n.created_by
      AND r.role_code IN ('counselor', 'admin')
)
AND EXISTS (
    SELECT 1
    FROM t_user u
    INNER JOIN t_user_role ur ON ur.user_id = u.id
    INNER JOIN t_role r ON r.id = ur.role_id
    WHERE r.role_code = 'counselor'
      AND u.status = 1
);

UPDATE notice_feedback f
SET assigned_counselor_id = n.feedback_counselor_id
FROM notice n
WHERE f.notice_id = n.id
  AND (
      f.assigned_counselor_id IS NULL
      OR f.assigned_counselor_id <> n.feedback_counselor_id
  )
  AND EXISTS (
      SELECT 1
      FROM t_user_role ur
      INNER JOIN t_role r ON r.id = ur.role_id
      WHERE ur.user_id = n.feedback_counselor_id
        AND r.role_code IN ('counselor', 'admin')
  );
