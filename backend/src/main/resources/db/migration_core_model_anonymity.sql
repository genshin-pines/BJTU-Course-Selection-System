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
    legacy_course_id    BIGINT COMMENT 'legacy course id',
    teacher_id          BIGINT NOT NULL COMMENT 'teacher id',
    semester            VARCHAR(50) DEFAULT 'UNKNOWN' COMMENT 'semester',
    class_name          VARCHAR(100) COMMENT 'class name',
    avg_score           DOUBLE DEFAULT 0 COMMENT 'average overall score',
    grading_score       DOUBLE DEFAULT 0 COMMENT 'average grading score',
    avg_teaching_score  DOUBLE DEFAULT 0 COMMENT 'average teaching score',
    avg_workload_score  DOUBLE DEFAULT 0 COMMENT 'average workload score',
    review_count        INT    DEFAULT 0 COMMENT 'published review count',
    UNIQUE KEY uk_course_instance_legacy (legacy_course_id),
    INDEX idx_course_instance_base (course_base_id),
    INDEX idx_course_instance_teacher (teacher_id),
    INDEX idx_course_instance_semester (semester)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT 'course instance';

CREATE TABLE IF NOT EXISTS course_teacher_relation
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_base_id BIGINT NOT NULL COMMENT 'course base id',
    teacher_id     BIGINT NOT NULL COMMENT 'teacher id',
    semester       VARCHAR(50) DEFAULT 'UNKNOWN' COMMENT 'semester',
    UNIQUE KEY uk_course_teacher_semester (course_base_id, teacher_id, semester),
    INDEX idx_course_teacher_base (course_base_id),
    INDEX idx_course_teacher_teacher (teacher_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT 'course teacher relation';

CREATE TABLE IF NOT EXISTS voter_record
(
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id          BIGINT NOT NULL COMMENT 'private identity mapping, do not expose to review queries',
    anonymous_key       VARCHAR(64) NOT NULL COMMENT 'irreversible anonymous key',
    display_name        VARCHAR(80) NOT NULL COMMENT 'anonymous display name',
    scope_type          VARCHAR(40) NOT NULL COMMENT 'anonymous scope',
    course_id           BIGINT NOT NULL COMMENT 'legacy course id',
    teacher_id          BIGINT NOT NULL COMMENT 'teacher id',
    course_instance_id  BIGINT NOT NULL DEFAULT 0 COMMENT 'course instance id, 0 means legacy course scope',
    create_time         DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    UNIQUE KEY uk_voter_scope (student_id, scope_type, course_id, teacher_id, course_instance_id),
    UNIQUE KEY uk_voter_anonymous_key (anonymous_key),
    INDEX idx_voter_course_teacher (course_id, teacher_id),
    INDEX idx_voter_instance (course_instance_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT 'anonymous voter record';

INSERT INTO course_base (course_code, course_name, credit, department)
SELECT c.course_code, c.course_name, c.credit, c.department
FROM course c
WHERE NOT EXISTS (
    SELECT 1 FROM course_base cb WHERE cb.course_code = c.course_code
);

INSERT INTO course_instance (
    course_base_id, legacy_course_id, teacher_id, semester, avg_score,
    grading_score, avg_teaching_score, avg_workload_score, review_count
)
SELECT cb.id, c.id, c.teacher_id, 'UNKNOWN', c.avg_score,
       c.grading_score, c.avg_teaching_score, c.avg_workload_score, c.review_count
FROM course c
JOIN course_base cb ON cb.course_code = c.course_code
WHERE c.teacher_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM course_instance ci WHERE ci.legacy_course_id = c.id
  );

INSERT INTO course_teacher_relation (course_base_id, teacher_id, semester)
SELECT DISTINCT cb.id, c.teacher_id, 'UNKNOWN'
FROM course c
JOIN course_base cb ON cb.course_code = c.course_code
WHERE c.teacher_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM course_teacher_relation ctr
      WHERE ctr.course_base_id = cb.id
        AND ctr.teacher_id = c.teacher_id
        AND ctr.semester = 'UNKNOWN'
  );

SET @review_student_nullable_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'review'
              AND COLUMN_NAME = 'student_id'
              AND IS_NULLABLE = 'NO'
        ),
        'ALTER TABLE review MODIFY COLUMN student_id BIGINT NULL COMMENT ''legacy student id, nullable after anonymity migration''',
        'SELECT ''review.student_id already nullable'''
    )
);
PREPARE review_student_nullable_stmt FROM @review_student_nullable_sql;
EXECUTE review_student_nullable_stmt;
DEALLOCATE PREPARE review_student_nullable_stmt;

SET @review_voter_record_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'review'
              AND COLUMN_NAME = 'voter_record_id'
        ),
        'ALTER TABLE review ADD COLUMN voter_record_id BIGINT NULL COMMENT ''anonymous voter record id''',
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
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'review'
              AND COLUMN_NAME = 'anonymous_user_key'
        ),
        'ALTER TABLE review ADD COLUMN anonymous_user_key VARCHAR(64) NULL COMMENT ''anonymous user key snapshot''',
        'SELECT ''review.anonymous_user_key already exists'''
    )
);
PREPARE review_anonymous_key_stmt FROM @review_anonymous_key_sql;
EXECUTE review_anonymous_key_stmt;
DEALLOCATE PREPARE review_anonymous_key_stmt;

SET @review_course_instance_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'review'
              AND COLUMN_NAME = 'course_instance_id'
        ),
        'ALTER TABLE review ADD COLUMN course_instance_id BIGINT NULL COMMENT ''course instance id''',
        'SELECT ''review.course_instance_id already exists'''
    )
);
PREPARE review_course_instance_stmt FROM @review_course_instance_sql;
EXECUTE review_course_instance_stmt;
DEALLOCATE PREPARE review_course_instance_stmt;

SET @vote_student_nullable_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'review_vote'
              AND COLUMN_NAME = 'student_id'
              AND IS_NULLABLE = 'NO'
        ),
        'ALTER TABLE review_vote MODIFY COLUMN student_id BIGINT NULL COMMENT ''legacy student id, nullable after anonymity migration''',
        'SELECT ''review_vote.student_id already nullable'''
    )
);
PREPARE vote_student_nullable_stmt FROM @vote_student_nullable_sql;
EXECUTE vote_student_nullable_stmt;
DEALLOCATE PREPARE vote_student_nullable_stmt;

SET @vote_voter_record_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'review_vote'
              AND COLUMN_NAME = 'voter_record_id'
        ),
        'ALTER TABLE review_vote ADD COLUMN voter_record_id BIGINT NULL COMMENT ''anonymous voter record id''',
        'SELECT ''review_vote.voter_record_id already exists'''
    )
);
PREPARE vote_voter_record_stmt FROM @vote_voter_record_sql;
EXECUTE vote_voter_record_stmt;
DEALLOCATE PREPARE vote_voter_record_stmt;

SET @vote_type_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'review_vote'
              AND COLUMN_NAME = 'vote_type'
        ),
        'ALTER TABLE review_vote ADD COLUMN vote_type VARCHAR(20) NOT NULL DEFAULT ''UPVOTE'' COMMENT ''vote type''',
        'SELECT ''review_vote.vote_type already exists'''
    )
);
PREPARE vote_type_stmt FROM @vote_type_sql;
EXECUTE vote_type_stmt;
DEALLOCATE PREPARE vote_type_stmt;

SET @reporter_nullable_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'report'
              AND COLUMN_NAME = 'reporter_id'
              AND IS_NULLABLE = 'NO'
        ),
        'ALTER TABLE report MODIFY COLUMN reporter_id BIGINT NULL COMMENT ''legacy reporter id, nullable after anonymity migration''',
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
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'report'
              AND COLUMN_NAME = 'reporter_record_id'
        ),
        'ALTER TABLE report ADD COLUMN reporter_record_id BIGINT NULL COMMENT ''anonymous reporter record id''',
        'SELECT ''report.reporter_record_id already exists'''
    )
);
PREPARE report_record_stmt FROM @report_record_sql;
EXECUTE report_record_stmt;
DEALLOCATE PREPARE report_record_stmt;

SET @idx_review_voter_record_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'review'
              AND INDEX_NAME = 'idx_review_voter_record'
        ),
        'CREATE INDEX idx_review_voter_record ON review (voter_record_id)',
        'SELECT ''idx_review_voter_record already exists'''
    )
);
PREPARE idx_review_voter_record_stmt FROM @idx_review_voter_record_sql;
EXECUTE idx_review_voter_record_stmt;
DEALLOCATE PREPARE idx_review_voter_record_stmt;

SET @idx_review_course_instance_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'review'
              AND INDEX_NAME = 'idx_review_course_instance'
        ),
        'CREATE INDEX idx_review_course_instance ON review (course_instance_id)',
        'SELECT ''idx_review_course_instance already exists'''
    )
);
PREPARE idx_review_course_instance_stmt FROM @idx_review_course_instance_sql;
EXECUTE idx_review_course_instance_stmt;
DEALLOCATE PREPARE idx_review_course_instance_stmt;

SET @idx_vote_voter_record_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'review_vote'
              AND INDEX_NAME = 'idx_review_vote_voter_record'
        ),
        'CREATE INDEX idx_review_vote_voter_record ON review_vote (voter_record_id)',
        'SELECT ''idx_review_vote_voter_record already exists'''
    )
);
PREPARE idx_vote_voter_record_stmt FROM @idx_vote_voter_record_sql;
EXECUTE idx_vote_voter_record_stmt;
DEALLOCATE PREPARE idx_vote_voter_record_stmt;

SET @idx_report_record_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'report'
              AND INDEX_NAME = 'idx_report_reporter_record'
        ),
        'CREATE INDEX idx_report_reporter_record ON report (reporter_record_id)',
        'SELECT ''idx_report_reporter_record already exists'''
    )
);
PREPARE idx_report_record_stmt FROM @idx_report_record_sql;
EXECUTE idx_report_record_stmt;
DEALLOCATE PREPARE idx_report_record_stmt;
