USE bjtu_review;

SET @admin_role_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'admin'
      AND COLUMN_NAME = 'role'
);

SET @admin_role_sql = IF(
    @admin_role_column_exists = 0,
    'ALTER TABLE admin ADD COLUMN role VARCHAR(30) NOT NULL DEFAULT ''SUPER_ADMIN'' COMMENT ''管理员角色：SUPER_ADMIN/DEPT_OP/AUDITOR'' AFTER password',
    'SELECT ''admin.role already exists'''
);

PREPARE admin_role_stmt FROM @admin_role_sql;
EXECUTE admin_role_stmt;
DEALLOCATE PREPARE admin_role_stmt;

UPDATE admin
SET role = 'SUPER_ADMIN'
WHERE role IS NULL OR role = '';

INSERT INTO admin (username, password, role)
VALUES
    ('dept_op', '$2a$10$K2ylcuE/FXU5Q2bDxnDdbe2pbH2TmB/XywhrzwnrTMhpCnQpF2jjy', 'DEPT_OP'),
    ('auditor', '$2a$10$K2ylcuE/FXU5Q2bDxnDdbe2pbH2TmB/XywhrzwnrTMhpCnQpF2jjy', 'AUDITOR')
ON DUPLICATE KEY UPDATE
    role = VALUES(role);
