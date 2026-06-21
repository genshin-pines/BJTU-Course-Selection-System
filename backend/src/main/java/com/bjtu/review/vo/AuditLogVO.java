package com.bjtu.review.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLogVO {
    private Long id;
    private Long adminId;
    private Long reviewId;
    private Long reportId;
    private String operateType;
    private String reason;
    private LocalDateTime createTime;
    private Long courseInstanceId;
    private Long courseId;
    private String courseName;
    private Long teacherId;
    private String teacherName;
    private String anonymousId;
    private String reviewContent;
}
