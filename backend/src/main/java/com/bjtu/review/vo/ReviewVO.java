package com.bjtu.review.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReviewVO {
    private Long id;
    private String anonymousId;
    private Long voterRecordId;
    private String anonymousUserKey;
    private Long courseInstanceId;
    private Long courseId;
    private String courseName;
    private Long teacherId;
    private String teacherName;
    private Integer overallScore;
    private Integer gradingScore;
    private Integer teachingScore;
    private Integer workloadScore;
    private String content;
    private String studyTips;
    private String examType;
    private String keyChapters;
    private Boolean cheatSheetAllowed;
    private Integer likeCount;
    private Integer downvoteCount;
    private String status;
    private LocalDateTime createTime;
    private List<TagVO> tags;
    private Boolean isOwner;
}
