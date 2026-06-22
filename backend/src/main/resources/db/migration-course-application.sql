-- =============================================
-- 课程申请表（学生申请创建课程 + 评价）
-- 执行此脚本前请先 USE bjtu_review;
-- =============================================

CREATE TABLE IF NOT EXISTS course_application
(
    id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id           BIGINT       NOT NULL COMMENT '申请人学生ID',
    -- 课程信息
    course_code          VARCHAR(50)  NOT NULL COMMENT '课程代码',
    course_name          VARCHAR(200) NOT NULL COMMENT '课程名称',
    credit               INT          DEFAULT 0 COMMENT '学分',
    department           VARCHAR(100) COMMENT '学院',
    -- 教师信息
    teacher_name         VARCHAR(100) NOT NULL COMMENT '教师姓名',
    teacher_department   VARCHAR(100) COMMENT '教师所属学院',
    -- 评价信息
    grading_score        INT  NOT NULL COMMENT '给分评分',
    teaching_score       INT  NOT NULL COMMENT '授课质量评分',
    workload_score       INT  NOT NULL COMMENT '作业轻松度评分',
    content              TEXT NOT NULL COMMENT '评价内容',
    study_tips           TEXT COMMENT '学习建议',
    exam_type            VARCHAR(200) COMMENT '考核方式',
    key_chapters         TEXT COMMENT '重点章节',
    cheat_sheet_allowed  TINYINT(1) COMMENT '可带资料',
    tag_ids              VARCHAR(500) COMMENT '标签ID逗号分隔',
    -- 审核状态
    status               VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    review_reason        VARCHAR(500) COMMENT '审核备注',
    reviewer_admin_id    BIGINT COMMENT '审核管理员ID',
    created_instance_id  BIGINT COMMENT '审核通过后创建的开课实例ID',
    created_review_id    BIGINT COMMENT '审核通过后创建的评价ID',
    create_time          DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    review_time          DATETIME COMMENT '审核时间',
    INDEX idx_ca_status (status),
    INDEX idx_ca_student (student_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '学生课程申请表';
