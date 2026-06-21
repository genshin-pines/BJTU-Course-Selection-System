-- =============================================
-- BJTU 课程评价系统 数据库建表脚本
-- =============================================

CREATE DATABASE IF NOT EXISTS bjtu_review
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE bjtu_review;

-- 学生表
CREATE TABLE IF NOT EXISTS student
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_no   VARCHAR(20)  NOT NULL UNIQUE COMMENT '学号',
    name         VARCHAR(50)  NOT NULL COMMENT '姓名',
    anonymous_id VARCHAR(50)  NOT NULL COMMENT '匿名ID',
    password     VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    current_session_id VARCHAR(64) COMMENT '当前有效登录会话ID',
    major        VARCHAR(100) COMMENT '专业',
    grade        VARCHAR(20) COMMENT '年级',
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '学生表';

-- 教师表
CREATE TABLE IF NOT EXISTS teacher
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    teacher_name VARCHAR(100) NOT NULL COMMENT '教师姓名',
    department   VARCHAR(100) COMMENT '所属学院',
    avg_score         DOUBLE DEFAULT 0 COMMENT '平均综合评分',
    avg_teaching_score DOUBLE DEFAULT 0 COMMENT '平均授课质量评分',
    avg_workload_score DOUBLE DEFAULT 0 COMMENT '平均作业轻松度评分'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '教师表';

-- 课程表
CREATE TABLE IF NOT EXISTS course
(
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_code      VARCHAR(50)  NOT NULL COMMENT '课程代码',
    course_name      VARCHAR(200) NOT NULL COMMENT '课程名称',
    credit           INT          DEFAULT 0 COMMENT '学分',
    department       VARCHAR(100) COMMENT '开课学院',
    teacher_id       BIGINT COMMENT '授课教师ID',
    avg_score         DOUBLE       DEFAULT 0 COMMENT '平均综合评分',
    grading_score     DOUBLE       DEFAULT 0 COMMENT '平均给分评分',
    avg_teaching_score DOUBLE     DEFAULT 0 COMMENT '平均授课质量评分',
    avg_workload_score DOUBLE     DEFAULT 0 COMMENT '平均作业轻松度评分',
    review_count     INT          DEFAULT 0 COMMENT '评价数量',
    INDEX idx_course_name (course_name),
    INDEX idx_course_code (course_code),
    INDEX idx_department (department),
    INDEX idx_teacher_id (teacher_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '课程表';

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

-- 评价表
CREATE TABLE IF NOT EXISTS review
(
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id       BIGINT COMMENT 'legacy student id',
    voter_record_id  BIGINT COMMENT 'anonymous voter record id',
    anonymous_user_key VARCHAR(64) COMMENT 'anonymous user key snapshot',
    course_instance_id BIGINT COMMENT 'course instance id',
    course_id        BIGINT       NOT NULL COMMENT '课程ID',
    teacher_id       BIGINT       NOT NULL COMMENT '教师ID',
    overall_score    INT          NOT NULL COMMENT '综合评分 1-5（自动计算：三项平均分四舍五入）',
    grading_score    INT          NOT NULL COMMENT '给分评分 1-5',
    teaching_score   INT          NOT NULL DEFAULT 3 COMMENT '授课质量评分 1-5',
    workload_score   INT          NOT NULL DEFAULT 3 COMMENT '作业轻松度评分 1-5',
    content          TEXT COMMENT '评价内容',
    study_tips       TEXT COMMENT '学习建议',
    exam_type        VARCHAR(200) COMMENT '考核方式',
    like_count       INT          DEFAULT 0 COMMENT '点赞数',
    downvote_count   INT          NOT NULL DEFAULT 0 COMMENT '没用数',
    status           VARCHAR(20)  DEFAULT 'PENDING_AUDIT' COMMENT '状态: PENDING_AUDIT/PENDING_MANUAL/PUBLISHED/HIDDEN/ARCHIVED',
    hide_reason      VARCHAR(500) COMMENT '隐藏原因',
    create_time      DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_course_id (course_id),
    INDEX idx_teacher_id (teacher_id),
    INDEX idx_student_id (student_id),
    INDEX idx_review_voter_record (voter_record_id),
    INDEX idx_review_course_instance (course_instance_id),
    INDEX idx_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '评价表';

-- 标签表
CREATE TABLE IF NOT EXISTS tag
(
    id       BIGINT PRIMARY KEY AUTO_INCREMENT,
    tag_name VARCHAR(100) NOT NULL COMMENT '标签名称'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '标签表';

-- 评价-标签关联表
CREATE TABLE IF NOT EXISTS review_tag
(
    id        BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id BIGINT NOT NULL COMMENT '评价ID',
    tag_id    BIGINT NOT NULL COMMENT '标签ID',
    INDEX idx_review_id (review_id),
    INDEX idx_tag_id (tag_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '评价标签关联表';

-- 评价考试经验表
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

-- 评价点赞记录表
CREATE TABLE IF NOT EXISTS review_vote
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id   BIGINT NOT NULL COMMENT '评价ID',
    student_id  BIGINT COMMENT 'legacy student id',
    voter_record_id BIGINT COMMENT 'anonymous voter record id',
    vote_type   VARCHAR(20) NOT NULL DEFAULT 'UPVOTE' COMMENT 'vote type',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_review_vote_student (review_id, student_id),
    UNIQUE KEY uk_review_vote_record (review_id, voter_record_id),
    INDEX idx_review_vote_review_id (review_id),
    INDEX idx_review_vote_student_id (student_id),
    INDEX idx_review_vote_voter_record (voter_record_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '评价点赞记录表';

-- 举报表
CREATE TABLE IF NOT EXISTS report
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id   BIGINT       NOT NULL COMMENT '被举报评价ID',
    reporter_id BIGINT COMMENT 'legacy reporter id',
    reporter_record_id BIGINT COMMENT 'anonymous reporter record id',
    reason      TEXT         NOT NULL COMMENT '举报原因',
    status      VARCHAR(20)  DEFAULT 'PENDING' COMMENT '状态: PENDING/RESOLVED/DISMISSED',
    create_time DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_review_id (review_id),
    INDEX idx_status (status),
    INDEX idx_report_reporter_record (reporter_record_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '举报表';

-- 审计日志表
CREATE TABLE IF NOT EXISTS audit_log
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    admin_id     BIGINT      NOT NULL COMMENT '管理员ID',
    review_id    BIGINT COMMENT '评价ID',
    report_id    BIGINT COMMENT '举报ID',
    operate_type VARCHAR(50) NOT NULL COMMENT '操作类型',
    reason       TEXT COMMENT '操作原因',
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_admin_id (admin_id),
    INDEX idx_review_id (review_id),
    INDEX idx_report_id (report_id),
    INDEX idx_operate_type (operate_type),
    INDEX idx_create_time (create_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '审计日志表';

-- 管理员表
CREATE TABLE IF NOT EXISTS admin
(
    id       BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    role     VARCHAR(30)  NOT NULL DEFAULT 'SUPER_ADMIN' COMMENT '管理员角色：SUPER_ADMIN/DEPT_OP/AUDITOR',
    department VARCHAR(100) COMMENT '管理员所属院系',
    current_session_id VARCHAR(64) COMMENT '当前有效登录会话ID'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '管理员表';

-- =============================================
-- 初始数据
-- =============================================

-- 管理员账号（密码 123456）
INSERT INTO admin (username, password, role, department) VALUES
    ('admin', '$2a$10$K2ylcuE/FXU5Q2bDxnDdbe2pbH2TmB/XywhrzwnrTMhpCnQpF2jjy', 'SUPER_ADMIN', NULL),
    ('dept_op', '$2a$10$K2ylcuE/FXU5Q2bDxnDdbe2pbH2TmB/XywhrzwnrTMhpCnQpF2jjy', 'DEPT_OP', CONVERT(0xE8AEA1E7AE97E69CBAE4B88EE4BFA1E681AFE68A80E69CAFE5ADA6E999A2 USING utf8mb4)),
    ('auditor', '$2a$10$K2ylcuE/FXU5Q2bDxnDdbe2pbH2TmB/XywhrzwnrTMhpCnQpF2jjy', 'AUDITOR', NULL);

-- 预设标签
INSERT INTO tag (tag_name) VALUES
    ('给分好'), ('课程有趣'), ('老师负责'), ('作业少'),
    ('考试简单'), ('干货多'), ('点名少'), ('水课');

-- 示例学生（密码均为 123456，BCrypt加密）
INSERT INTO student (student_no, name, anonymous_id, password, major, grade) VALUES
    ('2022111111', '张三', '匿名用户A001', '$2a$10$K2ylcuE/FXU5Q2bDxnDdbe2pbH2TmB/XywhrzwnrTMhpCnQpF2jjy', '计算机科学与技术', '2022'),
    ('2022111112', '李四', '匿名用户B002', '$2a$10$K2ylcuE/FXU5Q2bDxnDdbe2pbH2TmB/XywhrzwnrTMhpCnQpF2jjy', '软件工程', '2022');

-- 示例教师
INSERT INTO teacher (teacher_name, department) VALUES
    ('王教授', '计算机与信息技术学院'),
    ('李教授', '软件学院'),
    ('张教授', '电子信息工程学院');

-- 示例课程
INSERT INTO course (course_code, course_name, credit, department, teacher_id) VALUES
    ('CST301', '数据库系统原理', 3, '计算机与信息技术学院', 1),
    ('CST302', '操作系统', 4, '计算机与信息技术学院', 1),
    ('SWE201', '软件工程', 3, '软件学院', 2),
    ('EEE101', '信号与系统', 4, '电子信息工程学院', 3);
