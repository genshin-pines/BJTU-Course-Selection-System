USE bjtu_review;

-- Backfill anonymous voter records for legacy reviews.
INSERT INTO voter_record (
    student_id, anonymous_key, display_name, scope_type,
    course_id, teacher_id, course_instance_id
)
SELECT DISTINCT
    r.student_id,
    SHA2(CONCAT('bjtu-review-anonymous-v1:', r.student_id, ':', r.course_id, ':', r.teacher_id, ':',
                COALESCE(r.course_instance_id, ci.id, 0)), 256) AS anonymous_key,
    COALESCE(s.anonymous_id, CONCAT('匿名用户', UPPER(LEFT(SHA2(CONCAT('bjtu-review-anonymous-v1:', r.student_id, ':', r.course_id, ':', r.teacher_id, ':',
                COALESCE(r.course_instance_id, ci.id, 0)), 256), 8)))) AS display_name,
    'COURSE_REVIEW',
    r.course_id,
    r.teacher_id,
    COALESCE(r.course_instance_id, ci.id, 0)
FROM review r
LEFT JOIN student s ON s.id = r.student_id
LEFT JOIN course_instance ci ON ci.legacy_course_id = r.course_id
WHERE r.student_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM voter_record vr
      WHERE vr.student_id = r.student_id
        AND vr.scope_type = 'COURSE_REVIEW'
        AND vr.course_id = r.course_id
        AND vr.teacher_id = r.teacher_id
        AND vr.course_instance_id = COALESCE(r.course_instance_id, ci.id, 0)
  );

UPDATE review r
LEFT JOIN course_instance ci ON ci.legacy_course_id = r.course_id
JOIN voter_record vr
  ON vr.student_id = r.student_id
 AND vr.scope_type = 'COURSE_REVIEW'
 AND vr.course_id = r.course_id
 AND vr.teacher_id = r.teacher_id
 AND vr.course_instance_id = COALESCE(r.course_instance_id, ci.id, 0)
SET r.voter_record_id = vr.id,
    r.anonymous_user_key = vr.anonymous_key,
    r.course_instance_id = COALESCE(r.course_instance_id, ci.id, 0)
WHERE r.student_id IS NOT NULL
  AND r.voter_record_id IS NULL;

-- Backfill anonymous voter records for legacy votes. A voter may differ from the review author.
INSERT INTO voter_record (
    student_id, anonymous_key, display_name, scope_type,
    course_id, teacher_id, course_instance_id
)
SELECT DISTINCT
    rv.student_id,
    SHA2(CONCAT('bjtu-review-anonymous-v1:', rv.student_id, ':', r.course_id, ':', r.teacher_id, ':',
                COALESCE(r.course_instance_id, ci.id, 0)), 256) AS anonymous_key,
    COALESCE(s.anonymous_id, CONCAT('匿名用户', UPPER(LEFT(SHA2(CONCAT('bjtu-review-anonymous-v1:', rv.student_id, ':', r.course_id, ':', r.teacher_id, ':',
                COALESCE(r.course_instance_id, ci.id, 0)), 256), 8)))) AS display_name,
    'COURSE_REVIEW',
    r.course_id,
    r.teacher_id,
    COALESCE(r.course_instance_id, ci.id, 0)
FROM review_vote rv
JOIN review r ON r.id = rv.review_id
LEFT JOIN student s ON s.id = rv.student_id
LEFT JOIN course_instance ci ON ci.legacy_course_id = r.course_id
WHERE rv.student_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM voter_record vr
      WHERE vr.student_id = rv.student_id
        AND vr.scope_type = 'COURSE_REVIEW'
        AND vr.course_id = r.course_id
        AND vr.teacher_id = r.teacher_id
        AND vr.course_instance_id = COALESCE(r.course_instance_id, ci.id, 0)
  );

UPDATE review_vote rv
JOIN review r ON r.id = rv.review_id
LEFT JOIN course_instance ci ON ci.legacy_course_id = r.course_id
JOIN voter_record vr
  ON vr.student_id = rv.student_id
 AND vr.scope_type = 'COURSE_REVIEW'
 AND vr.course_id = r.course_id
 AND vr.teacher_id = r.teacher_id
 AND vr.course_instance_id = COALESCE(r.course_instance_id, ci.id, 0)
SET rv.voter_record_id = vr.id,
    rv.vote_type = COALESCE(rv.vote_type, 'UPVOTE')
WHERE rv.student_id IS NOT NULL
  AND rv.voter_record_id IS NULL;

-- Backfill anonymous reporter records for legacy reports.
INSERT INTO voter_record (
    student_id, anonymous_key, display_name, scope_type,
    course_id, teacher_id, course_instance_id
)
SELECT DISTINCT
    rp.reporter_id,
    SHA2(CONCAT('bjtu-review-anonymous-v1:', rp.reporter_id, ':', r.course_id, ':', r.teacher_id, ':',
                COALESCE(r.course_instance_id, ci.id, 0)), 256) AS anonymous_key,
    COALESCE(s.anonymous_id, CONCAT('匿名用户', UPPER(LEFT(SHA2(CONCAT('bjtu-review-anonymous-v1:', rp.reporter_id, ':', r.course_id, ':', r.teacher_id, ':',
                COALESCE(r.course_instance_id, ci.id, 0)), 256), 8)))) AS display_name,
    'COURSE_REVIEW',
    r.course_id,
    r.teacher_id,
    COALESCE(r.course_instance_id, ci.id, 0)
FROM report rp
JOIN review r ON r.id = rp.review_id
LEFT JOIN student s ON s.id = rp.reporter_id
LEFT JOIN course_instance ci ON ci.legacy_course_id = r.course_id
WHERE rp.reporter_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM voter_record vr
      WHERE vr.student_id = rp.reporter_id
        AND vr.scope_type = 'COURSE_REVIEW'
        AND vr.course_id = r.course_id
        AND vr.teacher_id = r.teacher_id
        AND vr.course_instance_id = COALESCE(r.course_instance_id, ci.id, 0)
  );

UPDATE report rp
JOIN review r ON r.id = rp.review_id
LEFT JOIN course_instance ci ON ci.legacy_course_id = r.course_id
JOIN voter_record vr
  ON vr.student_id = rp.reporter_id
 AND vr.scope_type = 'COURSE_REVIEW'
 AND vr.course_id = r.course_id
 AND vr.teacher_id = r.teacher_id
 AND vr.course_instance_id = COALESCE(r.course_instance_id, ci.id, 0)
SET rp.reporter_record_id = vr.id
WHERE rp.reporter_id IS NOT NULL
  AND rp.reporter_record_id IS NULL;

-- Backfill orphan legacy reports whose review was already deleted.
INSERT INTO voter_record (
    student_id, anonymous_key, display_name, scope_type,
    course_id, teacher_id, course_instance_id
)
SELECT DISTINCT
    rp.reporter_id,
    SHA2(CONCAT('bjtu-review-anonymous-v1:legacy-report:', rp.reporter_id), 256) AS anonymous_key,
    COALESCE(s.anonymous_id, CONCAT('匿名用户', UPPER(LEFT(SHA2(CONCAT('bjtu-review-anonymous-v1:legacy-report:', rp.reporter_id), 256), 8)))) AS display_name,
    'LEGACY_REPORT',
    0,
    0,
    0
FROM report rp
LEFT JOIN review r ON r.id = rp.review_id
LEFT JOIN student s ON s.id = rp.reporter_id
WHERE rp.reporter_id IS NOT NULL
  AND rp.reporter_record_id IS NULL
  AND r.id IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM voter_record vr
      WHERE vr.student_id = rp.reporter_id
        AND vr.scope_type = 'LEGACY_REPORT'
        AND vr.course_id = 0
        AND vr.teacher_id = 0
        AND vr.course_instance_id = 0
  );

UPDATE report rp
LEFT JOIN review r ON r.id = rp.review_id
JOIN voter_record vr
  ON vr.student_id = rp.reporter_id
 AND vr.scope_type = 'LEGACY_REPORT'
 AND vr.course_id = 0
 AND vr.teacher_id = 0
 AND vr.course_instance_id = 0
SET rp.reporter_record_id = vr.id
WHERE rp.reporter_id IS NOT NULL
  AND rp.reporter_record_id IS NULL
  AND r.id IS NULL;

SET @uk_vote_voter_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'review_vote'
              AND INDEX_NAME = 'uk_review_vote_voter'
        ),
        'ALTER TABLE review_vote ADD UNIQUE KEY uk_review_vote_voter (review_id, voter_record_id, vote_type)',
        'SELECT ''uk_review_vote_voter already exists'''
    )
);
PREPARE uk_vote_voter_stmt FROM @uk_vote_voter_sql;
EXECUTE uk_vote_voter_stmt;
DEALLOCATE PREPARE uk_vote_voter_stmt;
