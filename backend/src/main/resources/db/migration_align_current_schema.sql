-- Align an existing bjtu_review database to the current course_base + course_instance model.
-- This script is safe to run after the older migration scripts, and is intended for
-- partially migrated local databases where runtime APIs fail because columns are missing.

CREATE DATABASE IF NOT EXISTS bjtu_review
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE bjtu_review;

CREATE TABLE IF NOT EXISTS course_base
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_code VARCHAR(50)  NOT NULL COMMENT 'course code',
    course_name VARCHAR(200) NOT NULL COMMENT 'course name',
    credit      INT DEFAULT 0 COMMENT 'credit',
    department  VARCHAR(100) COMMENT 'department',
    UNIQUE KEY uk_course_base_code (course_code),
    INDEX idx_course_base_name (course_name),
    INDEX idx_course_base_department (department)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT 'course base';

CREATE TABLE IF NOT EXISTS course_instance
(
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_base_id      BIGINT NOT NULL COMMENT 'course base id',
    teacher_id          BIGINT NOT NULL COMMENT 'teacher id',
    avg_score           DOUBLE DEFAULT 0 COMMENT 'average overall score',
    grading_score       DOUBLE DEFAULT 0 COMMENT 'average grading score',
    avg_teaching_score  DOUBLE DEFAULT 0 COMMENT 'average teaching score',
    avg_workload_score  DOUBLE DEFAULT 0 COMMENT 'average workload score',
    review_count        INT    DEFAULT 0 COMMENT 'published review count',
    INDEX idx_course_instance_base (course_base_id),
    INDEX idx_course_instance_teacher (teacher_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT 'course instance';

CREATE TABLE IF NOT EXISTS voter_record
(
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id          BIGINT NOT NULL COMMENT 'private identity mapping, do not expose to review queries',
    anonymous_key       VARCHAR(64) NOT NULL COMMENT 'irreversible anonymous key',
    display_name        VARCHAR(80) NOT NULL COMMENT 'anonymous display name',
    scope_type          VARCHAR(40) NOT NULL COMMENT 'anonymous scope',
    course_id           BIGINT NOT NULL COMMENT 'course base id',
    teacher_id          BIGINT NOT NULL COMMENT 'teacher id',
    course_instance_id  BIGINT NOT NULL DEFAULT 0 COMMENT 'course instance id',
    create_time         DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    UNIQUE KEY uk_voter_scope (student_id, scope_type, course_id, teacher_id, course_instance_id),
    UNIQUE KEY uk_voter_anonymous_key (anonymous_key),
    INDEX idx_voter_course_teacher (course_id, teacher_id),
    INDEX idx_voter_instance (course_instance_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT 'anonymous voter record';

CREATE TABLE IF NOT EXISTS review_exam_exp
(
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id           BIGINT NOT NULL COMMENT 'review id',
    exam_type           VARCHAR(200) COMMENT 'exam type',
    study_tips          TEXT COMMENT 'study tips',
    key_chapters        TEXT COMMENT 'key chapters',
    cheat_sheet_allowed TINYINT(1) COMMENT 'cheat sheet allowed',
    create_time         DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time         DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    UNIQUE KEY uk_review_exam_exp_review (review_id),
    INDEX idx_review_exam_exp_review_id (review_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT 'review exam experience';

SET @copy_legacy_course_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course'
        ),
        CONCAT(
            'INSERT IGNORE INTO course_base (course_code, course_name, credit, department) ',
            'SELECT course_code, course_name, COALESCE(credit, 0), department FROM course'
        ),
        'SELECT ''legacy course table absent'''
    )
);
PREPARE copy_legacy_course_stmt FROM @copy_legacy_course_sql;
EXECUTE copy_legacy_course_stmt;
DEALLOCATE PREPARE copy_legacy_course_stmt;

SET @copy_legacy_instance_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course'
        ),
        CONCAT(
            'INSERT INTO course_instance ',
            '(course_base_id, teacher_id, avg_score, grading_score, avg_teaching_score, avg_workload_score, review_count) ',
            'SELECT cb.id, c.teacher_id, COALESCE(c.avg_score, 0), COALESCE(c.grading_score, 0), ',
            'COALESCE(c.avg_teaching_score, 0), COALESCE(c.avg_workload_score, 0), COALESCE(c.review_count, 0) ',
            'FROM course c ',
            'JOIN course_base cb ON cb.course_code = c.course_code ',
            'WHERE c.teacher_id IS NOT NULL ',
            'AND NOT EXISTS (',
            'SELECT 1 FROM course_instance ci ',
            'WHERE ci.course_base_id = cb.id AND ci.teacher_id = c.teacher_id',
            ')'
        ),
        'SELECT ''legacy course table absent'''
    )
);
PREPARE copy_legacy_instance_stmt FROM @copy_legacy_instance_sql;
EXECUTE copy_legacy_instance_stmt;
DEALLOCATE PREPARE copy_legacy_instance_stmt;

SET @student_session_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'student' AND COLUMN_NAME = 'current_session_id'
        ),
        'ALTER TABLE student ADD COLUMN current_session_id VARCHAR(64) COMMENT ''current valid login session id''',
        'SELECT ''student.current_session_id already exists'''
    )
);
PREPARE student_session_stmt FROM @student_session_sql;
EXECUTE student_session_stmt;
DEALLOCATE PREPARE student_session_stmt;

SET @admin_role_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'admin' AND COLUMN_NAME = 'role'
        ),
        'ALTER TABLE admin ADD COLUMN role VARCHAR(30) NOT NULL DEFAULT ''SUPER_ADMIN'' COMMENT ''admin role''',
        'SELECT ''admin.role already exists'''
    )
);
PREPARE admin_role_stmt FROM @admin_role_sql;
EXECUTE admin_role_stmt;
DEALLOCATE PREPARE admin_role_stmt;

SET @admin_department_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'admin' AND COLUMN_NAME = 'department'
        ),
        'ALTER TABLE admin ADD COLUMN department VARCHAR(100) NULL COMMENT ''admin department''',
        'SELECT ''admin.department already exists'''
    )
);
PREPARE admin_department_stmt FROM @admin_department_sql;
EXECUTE admin_department_stmt;
DEALLOCATE PREPARE admin_department_stmt;

SET @admin_session_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'admin' AND COLUMN_NAME = 'current_session_id'
        ),
        'ALTER TABLE admin ADD COLUMN current_session_id VARCHAR(64) COMMENT ''current valid login session id''',
        'SELECT ''admin.current_session_id already exists'''
    )
);
PREPARE admin_session_stmt FROM @admin_session_sql;
EXECUTE admin_session_stmt;
DEALLOCATE PREPARE admin_session_stmt;

SET @review_student_nullable_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'review' AND COLUMN_NAME = 'student_id' AND IS_NULLABLE = 'NO'
        ),
        'ALTER TABLE review MODIFY COLUMN student_id BIGINT NULL COMMENT ''legacy student id''',
        'SELECT ''review.student_id already nullable or absent'''
    )
);
PREPARE review_student_nullable_stmt FROM @review_student_nullable_sql;
EXECUTE review_student_nullable_stmt;
DEALLOCATE PREPARE review_student_nullable_stmt;

SET @review_voter_record_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'review' AND COLUMN_NAME = 'voter_record_id'
        ),
        'ALTER TABLE review ADD COLUMN voter_record_id BIGINT NULL COMMENT ''anonymous voter record id'' AFTER student_id',
        'SELECT ''review.voter_record_id already exists'''
    )
);
PREPARE review_voter_record_stmt FROM @review_voter_record_sql;
EXECUTE review_voter_record_stmt;
DEALLOCATE PREPARE review_voter_record_stmt;

SET @review_anonymous_key_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'review' AND COLUMN_NAME = 'anonymous_user_key'
        ),
        'ALTER TABLE review ADD COLUMN anonymous_user_key VARCHAR(64) NULL COMMENT ''anonymous user key snapshot'' AFTER voter_record_id',
        'SELECT ''review.anonymous_user_key already exists'''
    )
);
PREPARE review_anonymous_key_stmt FROM @review_anonymous_key_sql;
EXECUTE review_anonymous_key_stmt;
DEALLOCATE PREPARE review_anonymous_key_stmt;

SET @review_instance_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'review' AND COLUMN_NAME = 'course_instance_id'
        ),
        'ALTER TABLE review ADD COLUMN course_instance_id BIGINT NULL COMMENT ''course instance id'' AFTER anonymous_user_key',
        'SELECT ''review.course_instance_id already exists'''
    )
);
PREPARE review_instance_stmt FROM @review_instance_sql;
EXECUTE review_instance_stmt;
DEALLOCATE PREPARE review_instance_stmt;

SET @review_teaching_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'review' AND COLUMN_NAME = 'teaching_score'
        ),
        'ALTER TABLE review ADD COLUMN teaching_score INT NOT NULL DEFAULT 3 COMMENT ''teaching score'' AFTER grading_score',
        'SELECT ''review.teaching_score already exists'''
    )
);
PREPARE review_teaching_stmt FROM @review_teaching_sql;
EXECUTE review_teaching_stmt;
DEALLOCATE PREPARE review_teaching_stmt;

SET @review_workload_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'review' AND COLUMN_NAME = 'workload_score'
        ),
        'ALTER TABLE review ADD COLUMN workload_score INT NOT NULL DEFAULT 3 COMMENT ''workload score'' AFTER teaching_score',
        'SELECT ''review.workload_score already exists'''
    )
);
PREPARE review_workload_stmt FROM @review_workload_sql;
EXECUTE review_workload_stmt;
DEALLOCATE PREPARE review_workload_stmt;

SET @review_downvote_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'review' AND COLUMN_NAME = 'downvote_count'
        ),
        'ALTER TABLE review ADD COLUMN downvote_count INT NOT NULL DEFAULT 0 COMMENT ''downvote count'' AFTER like_count',
        'SELECT ''review.downvote_count already exists'''
    )
);
PREPARE review_downvote_stmt FROM @review_downvote_sql;
EXECUTE review_downvote_stmt;
DEALLOCATE PREPARE review_downvote_stmt;

SET @review_hide_reason_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'review' AND COLUMN_NAME = 'hide_reason'
        ),
        'ALTER TABLE review ADD COLUMN hide_reason VARCHAR(500) NULL COMMENT ''hide reason'' AFTER status',
        'SELECT ''review.hide_reason already exists'''
    )
);
PREPARE review_hide_reason_stmt FROM @review_hide_reason_sql;
EXECUTE review_hide_reason_stmt;
DEALLOCATE PREPARE review_hide_reason_stmt;

SET @vote_student_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'review_vote' AND COLUMN_NAME = 'student_id'
        ),
        'ALTER TABLE review_vote ADD COLUMN student_id BIGINT NULL COMMENT ''legacy student id'' AFTER review_id',
        'SELECT ''review_vote.student_id already exists'''
    )
);
PREPARE vote_student_stmt FROM @vote_student_sql;
EXECUTE vote_student_stmt;
DEALLOCATE PREPARE vote_student_stmt;

SET @vote_student_nullable_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'review_vote' AND COLUMN_NAME = 'student_id' AND IS_NULLABLE = 'NO'
        ),
        'ALTER TABLE review_vote MODIFY COLUMN student_id BIGINT NULL COMMENT ''legacy student id''',
        'SELECT ''review_vote.student_id already nullable'''
    )
);
PREPARE vote_student_nullable_stmt FROM @vote_student_nullable_sql;
EXECUTE vote_student_nullable_stmt;
DEALLOCATE PREPARE vote_student_nullable_stmt;

SET @vote_record_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'review_vote' AND COLUMN_NAME = 'voter_record_id'
        ),
        'ALTER TABLE review_vote ADD COLUMN voter_record_id BIGINT NULL COMMENT ''anonymous voter record id'' AFTER student_id',
        'SELECT ''review_vote.voter_record_id already exists'''
    )
);
PREPARE vote_record_stmt FROM @vote_record_sql;
EXECUTE vote_record_stmt;
DEALLOCATE PREPARE vote_record_stmt;

SET @vote_type_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'review_vote' AND COLUMN_NAME = 'vote_type'
        ),
        'ALTER TABLE review_vote ADD COLUMN vote_type VARCHAR(20) NOT NULL DEFAULT ''UPVOTE'' COMMENT ''vote type'' AFTER voter_record_id',
        'SELECT ''review_vote.vote_type already exists'''
    )
);
PREPARE vote_type_stmt FROM @vote_type_sql;
EXECUTE vote_type_stmt;
DEALLOCATE PREPARE vote_type_stmt;

SET @reporter_id_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report' AND COLUMN_NAME = 'reporter_id'
        ),
        'ALTER TABLE report ADD COLUMN reporter_id BIGINT NULL COMMENT ''legacy reporter id'' AFTER review_id',
        'SELECT ''report.reporter_id already exists'''
    )
);
PREPARE reporter_id_stmt FROM @reporter_id_sql;
EXECUTE reporter_id_stmt;
DEALLOCATE PREPARE reporter_id_stmt;

SET @reporter_nullable_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report' AND COLUMN_NAME = 'reporter_id' AND IS_NULLABLE = 'NO'
        ),
        'ALTER TABLE report MODIFY COLUMN reporter_id BIGINT NULL COMMENT ''legacy reporter id''',
        'SELECT ''report.reporter_id already nullable'''
    )
);
PREPARE reporter_nullable_stmt FROM @reporter_nullable_sql;
EXECUTE reporter_nullable_stmt;
DEALLOCATE PREPARE reporter_nullable_stmt;

SET @report_record_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report' AND COLUMN_NAME = 'reporter_record_id'
        ),
        'ALTER TABLE report ADD COLUMN reporter_record_id BIGINT NULL COMMENT ''anonymous reporter record id'' AFTER reporter_id',
        'SELECT ''report.reporter_record_id already exists'''
    )
);
PREPARE report_record_stmt FROM @report_record_sql;
EXECUTE report_record_stmt;
DEALLOCATE PREPARE report_record_stmt;

UPDATE review r
JOIN course_instance ci
  ON ci.course_base_id = r.course_id
 AND ci.teacher_id = r.teacher_id
SET r.course_instance_id = ci.id
WHERE r.course_instance_id IS NULL;

SET @legacy_review_instance_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course_instance' AND COLUMN_NAME = 'legacy_course_id'
        ),
        CONCAT(
            'UPDATE review r ',
            'JOIN course_instance ci ON ci.legacy_course_id = r.course_id ',
            'SET r.course_instance_id = ci.id ',
            'WHERE r.course_instance_id IS NULL'
        ),
        'SELECT ''course_instance.legacy_course_id absent'''
    )
);
PREPARE legacy_review_instance_stmt FROM @legacy_review_instance_sql;
EXECUTE legacy_review_instance_stmt;
DEALLOCATE PREPARE legacy_review_instance_stmt;

UPDATE review r
JOIN course_instance ci ON ci.id = r.course_instance_id
SET r.course_id = ci.course_base_id
WHERE r.course_instance_id IS NOT NULL
  AND r.course_id <> ci.course_base_id;

INSERT IGNORE INTO voter_record
    (student_id, anonymous_key, display_name, scope_type, course_id, teacher_id, course_instance_id)
SELECT r.student_id,
       SHA2(CONCAT('review-anonymous-salt:', r.student_id, ':', COALESCE(ci.course_base_id, r.course_id), ':', r.teacher_id, ':', COALESCE(r.course_instance_id, 0)), 256),
       CONCAT('匿名用户', UPPER(SUBSTRING(SHA2(CONCAT('review-anonymous-salt:', r.student_id, ':', COALESCE(ci.course_base_id, r.course_id), ':', r.teacher_id, ':', COALESCE(r.course_instance_id, 0)), 256), 1, 8))),
       'COURSE_REVIEW',
       COALESCE(ci.course_base_id, r.course_id),
       r.teacher_id,
       COALESCE(r.course_instance_id, 0)
FROM review r
LEFT JOIN course_instance ci ON ci.id = r.course_instance_id
WHERE r.student_id IS NOT NULL;

UPDATE review r
LEFT JOIN course_instance ci ON ci.id = r.course_instance_id
JOIN voter_record vr
  ON vr.student_id = r.student_id
 AND vr.scope_type = 'COURSE_REVIEW'
 AND vr.course_id = COALESCE(ci.course_base_id, r.course_id)
 AND vr.teacher_id = r.teacher_id
 AND vr.course_instance_id = COALESCE(r.course_instance_id, 0)
SET r.voter_record_id = vr.id,
    r.anonymous_user_key = vr.anonymous_key
WHERE r.student_id IS NOT NULL
  AND r.voter_record_id IS NULL;

INSERT IGNORE INTO voter_record
    (student_id, anonymous_key, display_name, scope_type, course_id, teacher_id, course_instance_id)
SELECT rv.student_id,
       SHA2(CONCAT('review-anonymous-salt:', rv.student_id, ':', COALESCE(ci.course_base_id, r.course_id), ':', r.teacher_id, ':', COALESCE(r.course_instance_id, 0)), 256),
       CONCAT('匿名用户', UPPER(SUBSTRING(SHA2(CONCAT('review-anonymous-salt:', rv.student_id, ':', COALESCE(ci.course_base_id, r.course_id), ':', r.teacher_id, ':', COALESCE(r.course_instance_id, 0)), 256), 1, 8))),
       'COURSE_REVIEW',
       COALESCE(ci.course_base_id, r.course_id),
       r.teacher_id,
       COALESCE(r.course_instance_id, 0)
FROM review_vote rv
JOIN review r ON r.id = rv.review_id
LEFT JOIN course_instance ci ON ci.id = r.course_instance_id
WHERE rv.student_id IS NOT NULL
  AND rv.voter_record_id IS NULL;

UPDATE review_vote rv
JOIN review r ON r.id = rv.review_id
LEFT JOIN course_instance ci ON ci.id = r.course_instance_id
JOIN voter_record vr
  ON vr.student_id = rv.student_id
 AND vr.scope_type = 'COURSE_REVIEW'
 AND vr.course_id = COALESCE(ci.course_base_id, r.course_id)
 AND vr.teacher_id = r.teacher_id
 AND vr.course_instance_id = COALESCE(r.course_instance_id, 0)
SET rv.voter_record_id = vr.id
WHERE rv.student_id IS NOT NULL
  AND rv.voter_record_id IS NULL;

INSERT IGNORE INTO voter_record
    (student_id, anonymous_key, display_name, scope_type, course_id, teacher_id, course_instance_id)
SELECT rp.reporter_id,
       SHA2(CONCAT('review-anonymous-salt:', rp.reporter_id, ':', COALESCE(ci.course_base_id, r.course_id), ':', r.teacher_id, ':', COALESCE(r.course_instance_id, 0)), 256),
       CONCAT('匿名用户', UPPER(SUBSTRING(SHA2(CONCAT('review-anonymous-salt:', rp.reporter_id, ':', COALESCE(ci.course_base_id, r.course_id), ':', r.teacher_id, ':', COALESCE(r.course_instance_id, 0)), 256), 1, 8))),
       'COURSE_REVIEW',
       COALESCE(ci.course_base_id, r.course_id),
       r.teacher_id,
       COALESCE(r.course_instance_id, 0)
FROM report rp
JOIN review r ON r.id = rp.review_id
LEFT JOIN course_instance ci ON ci.id = r.course_instance_id
WHERE rp.reporter_id IS NOT NULL
  AND rp.reporter_record_id IS NULL;

UPDATE report rp
JOIN review r ON r.id = rp.review_id
LEFT JOIN course_instance ci ON ci.id = r.course_instance_id
JOIN voter_record vr
  ON vr.student_id = rp.reporter_id
 AND vr.scope_type = 'COURSE_REVIEW'
 AND vr.course_id = COALESCE(ci.course_base_id, r.course_id)
 AND vr.teacher_id = r.teacher_id
 AND vr.course_instance_id = COALESCE(r.course_instance_id, 0)
SET rp.reporter_record_id = vr.id
WHERE rp.reporter_id IS NOT NULL
  AND rp.reporter_record_id IS NULL;

INSERT INTO review_exam_exp (review_id, exam_type, study_tips)
SELECT r.id, r.exam_type, r.study_tips
FROM review r
LEFT JOIN review_exam_exp ree ON ree.review_id = r.id
WHERE ree.id IS NULL
  AND (
      NULLIF(TRIM(COALESCE(r.exam_type, '')), '') IS NOT NULL
      OR NULLIF(TRIM(COALESCE(r.study_tips, '')), '') IS NOT NULL
  );

UPDATE course_instance ci SET
    avg_score = (
        SELECT COALESCE(AVG((r.grading_score + r.teaching_score + r.workload_score) / 3.0), 0)
        FROM review r
        WHERE r.course_instance_id = ci.id AND r.status IN ('PUBLISHED', 'APPROVED')
    ),
    grading_score = (
        SELECT COALESCE(AVG(r.grading_score), 0)
        FROM review r
        WHERE r.course_instance_id = ci.id AND r.status IN ('PUBLISHED', 'APPROVED')
    ),
    avg_teaching_score = (
        SELECT COALESCE(AVG(r.teaching_score), 0)
        FROM review r
        WHERE r.course_instance_id = ci.id AND r.status IN ('PUBLISHED', 'APPROVED')
    ),
    avg_workload_score = (
        SELECT COALESCE(AVG(r.workload_score), 0)
        FROM review r
        WHERE r.course_instance_id = ci.id AND r.status IN ('PUBLISHED', 'APPROVED')
    ),
    review_count = (
        SELECT COUNT(*)
        FROM review r
        WHERE r.course_instance_id = ci.id AND r.status IN ('PUBLISHED', 'APPROVED')
    );

UPDATE teacher t SET
    avg_score = (
        SELECT COALESCE(AVG((r.grading_score + r.teaching_score + r.workload_score) / 3.0), 0)
        FROM review r
        WHERE r.teacher_id = t.id AND r.status IN ('PUBLISHED', 'APPROVED')
    ),
    avg_teaching_score = (
        SELECT COALESCE(AVG(r.teaching_score), 0)
        FROM review r
        WHERE r.teacher_id = t.id AND r.status IN ('PUBLISHED', 'APPROVED')
    ),
    avg_workload_score = (
        SELECT COALESCE(AVG(r.workload_score), 0)
        FROM review r
        WHERE r.teacher_id = t.id AND r.status IN ('PUBLISHED', 'APPROVED')
    );

SET @idx_review_voter_record_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'review' AND INDEX_NAME = 'idx_review_voter_record'
        ),
        'CREATE INDEX idx_review_voter_record ON review (voter_record_id)',
        'SELECT ''idx_review_voter_record already exists'''
    )
);
PREPARE idx_review_voter_record_stmt FROM @idx_review_voter_record_sql;
EXECUTE idx_review_voter_record_stmt;
DEALLOCATE PREPARE idx_review_voter_record_stmt;

SET @idx_review_instance_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'review' AND INDEX_NAME = 'idx_review_course_instance'
        ),
        'CREATE INDEX idx_review_course_instance ON review (course_instance_id)',
        'SELECT ''idx_review_course_instance already exists'''
    )
);
PREPARE idx_review_instance_stmt FROM @idx_review_instance_sql;
EXECUTE idx_review_instance_stmt;
DEALLOCATE PREPARE idx_review_instance_stmt;

SET @idx_vote_record_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'review_vote' AND INDEX_NAME = 'idx_review_vote_voter_record'
        ),
        'CREATE INDEX idx_review_vote_voter_record ON review_vote (voter_record_id)',
        'SELECT ''idx_review_vote_voter_record already exists'''
    )
);
PREPARE idx_vote_record_stmt FROM @idx_vote_record_sql;
EXECUTE idx_vote_record_stmt;
DEALLOCATE PREPARE idx_vote_record_stmt;

SET @idx_report_record_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report' AND INDEX_NAME = 'idx_report_reporter_record'
        ),
        'CREATE INDEX idx_report_reporter_record ON report (reporter_record_id)',
        'SELECT ''idx_report_reporter_record already exists'''
    )
);
PREPARE idx_report_record_stmt FROM @idx_report_record_sql;
EXECUTE idx_report_record_stmt;
DEALLOCATE PREPARE idx_report_record_stmt;
