package com.bjtu.review.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CourseApplicationVO {
    private Long id;
    private String studentAnonymousId;
    // 课程信息
    private String courseCode;
    private String courseName;
    private Integer credit;
    private String department;
    // 教师
    private String teacherName;
    // 评价
    private Integer gradingScore;
    private Integer teachingScore;
    private Integer workloadScore;
    private String content;
    private String studyTips;
    private String examType;
    private String keyChapters;
    private Boolean cheatSheetAllowed;
    private List<TagVO> tags;
    // 审核
    private String status;
    private String reviewReason;
    private Long createdInstanceId;
    private LocalDateTime createTime;
    private LocalDateTime reviewTime;
}
