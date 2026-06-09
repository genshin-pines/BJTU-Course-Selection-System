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

    // overallScore 由后端自动计算，前端无需传递
    private Integer overallScore;

    @NotNull(message = "给分评分不能为空")
    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    private Integer gradingScore;

    @NotNull(message = "授课质量评分不能为空")
    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    private Integer teachingScore;

    @NotNull(message = "作业轻松度评分不能为空")
    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    private Integer workloadScore;

    @NotBlank(message = "评价内容不能为空")
    private String content;

    private String studyTips;
    private String examType;
    private List<Long> tags;
}
