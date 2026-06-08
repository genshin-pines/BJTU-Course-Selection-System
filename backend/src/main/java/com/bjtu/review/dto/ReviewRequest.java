package com.bjtu.review.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class ReviewRequest {
    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    @NotNull(message = "教师ID不能为空")
    private Long teacherId;

    @NotNull(message = "综合评分不能为空")
    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    private Integer overallScore;

    @NotNull(message = "难度评分不能为空")
    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    private Integer difficultyScore;

    @NotNull(message = "给分评分不能为空")
    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    private Integer gradingScore;

    @NotBlank(message = "评价内容不能为空")
    private String content;

    private String studyTips;
    private String examType;
    private List<Long> tags;
}
