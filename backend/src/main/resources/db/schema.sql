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

-- 评价表
CREATE TABLE IF NOT EXISTS review
(
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id       BIGINT       NOT NULL COMMENT '学生ID',
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
    status           VARCHAR(20)  DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED',
    create_time      DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_course_id (course_id),
    INDEX idx_teacher_id (teacher_id),
    INDEX idx_student_id (student_id),
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

-- 举报表
CREATE TABLE IF NOT EXISTS report
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_id   BIGINT       NOT NULL COMMENT '被举报评价ID',
    reporter_id BIGINT       NOT NULL COMMENT '举报人ID',
    reason      TEXT         NOT NULL COMMENT '举报原因',
    status      VARCHAR(20)  DEFAULT 'PENDING' COMMENT '状态: PENDING/RESOLVED/DISMISSED',
    create_time DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_review_id (review_id),
    INDEX idx_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '举报表';

-- 管理员表
CREATE TABLE IF NOT EXISTS admin
(
    id       BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '管理员表';

-- =============================================
-- 初始数据
-- =============================================

-- 管理员账号
INSERT INTO admin (username, password) VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh');

-- 预设标签
INSERT INTO tag (tag_name) VALUES
    ('给分好'), ('课程有趣'), ('老师负责'), ('作业少'),
    ('考试简单'), ('干货多'), ('点名少'), ('水课');

-- 示例学生（密码均为 123456，BCrypt加密）
INSERT INTO student (student_no, name, anonymous_id, password, major, grade) VALUES
    ('2022111111', '张三', '匿名用户A001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '计算机科学与技术', '2022'),
    ('2022111112', '李四', '匿名用户B002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '软件工程', '2022');

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
