package com.bjtu.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReviewRequest {
    // Legacy course id. New instance-first flows may omit it.
    private Long courseId;

    private Long courseInstanceId;

    // Legacy teacher id. New instance-first flows derive it from courseInstanceId.
    private Long teacherId;

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
    private String keyChapters;
    private Boolean cheatSheetAllowed;
    private List<Long> tags;
}
