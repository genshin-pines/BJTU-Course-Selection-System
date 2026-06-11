USE bjtu_review;

SET @student_session_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'student'
      AND COLUMN_NAME = 'current_session_id'
);

SET @student_session_sql = IF(
    @student_session_column_exists = 0,
    'ALTER TABLE student ADD COLUMN current_session_id VARCHAR(64) COMMENT ''当前有效登录会话ID''',
    'SELECT ''student.current_session_id already exists'''
);

PREPARE student_session_stmt FROM @student_session_sql;
EXECUTE student_session_stmt;
DEALLOCATE PREPARE student_session_stmt;

SET @admin_session_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'admin'
      AND COLUMN_NAME = 'current_session_id'
);

SET @admin_session_sql = IF(
    @admin_session_column_exists = 0,
    'ALTER TABLE admin ADD COLUMN current_session_id VARCHAR(64) COMMENT ''当前有效登录会话ID''',
    'SELECT ''admin.current_session_id already exists'''
);

PREPARE admin_session_stmt FROM @admin_session_sql;
EXECUTE admin_session_stmt;
DEALLOCATE PREPARE admin_session_stmt;
