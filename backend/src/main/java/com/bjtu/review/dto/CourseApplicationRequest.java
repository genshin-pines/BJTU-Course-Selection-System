package com.bjtu.review.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class CourseApplicationRequest {
    // 课程信息
    @NotBlank(message = "课程代码不能为空")
    private String courseCode;

    @NotBlank(message = "课程名称不能为空")
    private String courseName;

    private Integer credit;

    private String department;

    // 教师信息
    @NotBlank(message = "教师姓名不能为空")
    private String teacherName;

    private String teacherDepartment;

    // 评价信息
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
    private List<Long> tagIds;
}
