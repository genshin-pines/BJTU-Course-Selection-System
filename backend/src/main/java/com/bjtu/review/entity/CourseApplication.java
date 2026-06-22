package com.bjtu.review.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("course_application")
public class CourseApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    // 课程信息
    private String courseCode;
    private String courseName;
    private Integer credit;
    private String department;
    // 教师信息
    private String teacherName;
    private String teacherDepartment;
    // 评价信息
    private Integer gradingScore;
    private Integer teachingScore;
    private Integer workloadScore;
    private String content;
    private String studyTips;
    private String examType;
    private String keyChapters;
    private Boolean cheatSheetAllowed;
    private String tagIds;
    // 审核
    private String status;
    private String reviewReason;
    private Long reviewerAdminId;
    private Long createdInstanceId;
    private Long createdReviewId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private LocalDateTime reviewTime;
}
