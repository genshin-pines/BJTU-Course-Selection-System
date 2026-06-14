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
    'ALTER TABLE admin ADD COLUMN role VARCHAR(30) NOT NULL DEFAULT ''SUPER_ADMIN'' AFTER password',
    'SELECT ''admin.role already exists'''
);

PREPARE admin_role_stmt FROM @admin_role_sql;
EXECUTE admin_role_stmt;
DEALLOCATE PREPARE admin_role_stmt;

SET @admin_department_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'admin'
      AND COLUMN_NAME = 'department'
);

SET @admin_department_sql = IF(
    @admin_department_column_exists = 0,
    'ALTER TABLE admin ADD COLUMN department VARCHAR(100) AFTER role',
    'SELECT ''admin.department already exists'''
);

PREPARE admin_department_stmt FROM @admin_department_sql;
EXECUTE admin_department_stmt;
DEALLOCATE PREPARE admin_department_stmt;

UPDATE admin
SET role = 'SUPER_ADMIN'
WHERE role IS NULL OR role = '';

INSERT INTO admin (username, password, role, department)
VALUES
    ('dept_op', '$2a$10$K2ylcuE/FXU5Q2bDxnDdbe2pbH2TmB/XywhrzwnrTMhpCnQpF2jjy', 'DEPT_OP', CONVERT(0xE8AEA1E7AE97E69CBAE4B88EE4BFA1E681AFE68A80E69CAFE5ADA6E999A2 USING utf8mb4)),
    ('auditor', '$2a$10$K2ylcuE/FXU5Q2bDxnDdbe2pbH2TmB/XywhrzwnrTMhpCnQpF2jjy', 'AUDITOR', NULL)
ON DUPLICATE KEY UPDATE
    role = VALUES(role),
    department = VALUES(department);
