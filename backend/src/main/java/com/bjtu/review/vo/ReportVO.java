package com.bjtu.review.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReportVO {
    private Long id;
    private Long reviewId;
    private String reviewContent;
    private Long courseInstanceId;
    private Long courseId;
    private String courseName;
    private Long teacherId;
    private String teacherName;
    private String anonymousId;
    private Long reporterRecordId;
    private String reporterAnonymousId;
    private String reason;
    private String status;
    private LocalDateTime createTime;
}
