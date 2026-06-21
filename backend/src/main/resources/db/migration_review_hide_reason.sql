-- Add hide_reason for auto/manual review hiding
USE bjtu_review;

ALTER TABLE review
    ADD COLUMN hide_reason VARCHAR(500) NULL COMMENT '隐藏原因' AFTER status;
