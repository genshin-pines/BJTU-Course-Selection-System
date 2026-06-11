USE bjtu_review;

CREATE TABLE IF NOT EXISTS review_vote
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id   BIGINT NOT NULL COMMENT 'review id',
    student_id  BIGINT NOT NULL COMMENT 'student id',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    UNIQUE KEY uk_review_vote_student (review_id, student_id),
    INDEX idx_review_vote_review_id (review_id),
    INDEX idx_review_vote_student_id (student_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT 'review vote';
