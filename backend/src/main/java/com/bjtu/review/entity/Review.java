package com.bjtu.review.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("review")
public class Review {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
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
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
