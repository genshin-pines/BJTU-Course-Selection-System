USE bjtu_review;

CREATE TABLE IF NOT EXISTS review_exam_exp
(
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id           BIGINT NOT NULL COMMENT '评价ID',
    exam_type           VARCHAR(200) COMMENT '考核方式',
    study_tips          TEXT COMMENT '学习/复习建议',
    key_chapters        TEXT COMMENT '重点章节',
    cheat_sheet_allowed TINYINT(1) COMMENT '是否允许带资料',
    create_time         DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_review_exam_exp_review (review_id),
    INDEX idx_review_exam_exp_review_id (review_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '评价考试经验表';

INSERT INTO review_exam_exp (review_id, exam_type, study_tips)
SELECT r.id, r.exam_type, r.study_tips
FROM review r
LEFT JOIN review_exam_exp ree ON ree.review_id = r.id
WHERE ree.id IS NULL
  AND (
      NULLIF(TRIM(COALESCE(r.exam_type, '')), '') IS NOT NULL
      OR NULLIF(TRIM(COALESCE(r.study_tips, '')), '') IS NOT NULL
  );
