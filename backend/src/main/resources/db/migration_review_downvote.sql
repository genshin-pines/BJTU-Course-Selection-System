USE bjtu_review;

SET @downvote_count_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'review'
              AND COLUMN_NAME = 'downvote_count'
        ),
        'ALTER TABLE review ADD COLUMN downvote_count INT NOT NULL DEFAULT 0 COMMENT ''downvote count'' AFTER like_count',
        'SELECT ''review.downvote_count already exists'''
    )
);
PREPARE downvote_count_stmt FROM @downvote_count_sql;
EXECUTE downvote_count_stmt;
DEALLOCATE PREPARE downvote_count_stmt;

UPDATE review r
LEFT JOIN (
    SELECT review_id,
           SUM(CASE WHEN vote_type = 'UPVOTE' THEN 1 ELSE 0 END) AS upvote_count,
           SUM(CASE WHEN vote_type = 'DOWNVOTE' THEN 1 ELSE 0 END) AS downvote_count
    FROM review_vote
    GROUP BY review_id
) vc ON vc.review_id = r.id
SET r.like_count = COALESCE(vc.upvote_count, 0),
    r.downvote_count = COALESCE(vc.downvote_count, 0);

SET @drop_vote_type_unique_sql = (
    SELECT IF(
        EXISTS (
            SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'review_vote'
              AND INDEX_NAME = 'uk_review_vote_voter'
        ),
        'ALTER TABLE review_vote DROP INDEX uk_review_vote_voter',
        'SELECT ''uk_review_vote_voter already absent'''
    )
);
PREPARE drop_vote_type_unique_stmt FROM @drop_vote_type_unique_sql;
EXECUTE drop_vote_type_unique_stmt;
DEALLOCATE PREPARE drop_vote_type_unique_stmt;

SET @add_active_vote_unique_sql = (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'review_vote'
              AND INDEX_NAME = 'uk_review_vote_active_voter'
        ),
        'ALTER TABLE review_vote ADD UNIQUE KEY uk_review_vote_active_voter (review_id, voter_record_id)',
        'SELECT ''uk_review_vote_active_voter already exists'''
    )
);
PREPARE add_active_vote_unique_stmt FROM @add_active_vote_unique_sql;
EXECUTE add_active_vote_unique_stmt;
DEALLOCATE PREPARE add_active_vote_unique_stmt;
