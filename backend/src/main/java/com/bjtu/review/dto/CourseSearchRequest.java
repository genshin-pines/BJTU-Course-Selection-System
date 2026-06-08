package com.bjtu.review.dto;

import lombok.Data;

@Data
public class CourseSearchRequest {
    private String keyword;
    private String department;
    private String teacherName;
    private Double minScore;
    private Double maxScore;
    private Integer difficultyLevel;
    private Integer gradingLevel;
    private Integer page = 1;
    private Integer size = 10;
}
