package com.bjtu.review.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("review")
public class Review {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long voterRecordId;
    private String anonymousUserKey;
    private Long courseInstanceId;
    private Long courseId;
    private Long teacherId;
    private Integer overallScore;
    private Integer gradingScore;
    private Integer teachingScore;
    private Integer workloadScore;
    private String content;
    private String studyTips;
    private String examType;
    private Integer likeCount;
    private Integer downvoteCount;
    private String status;
    private String hideReason;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
