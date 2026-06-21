package com.bjtu.review.dto;

import lombok.Data;

import java.util.List;

@Data
public class CourseSearchRequest {
    private String keyword;
    private String department;
    private String teacherName;
    private Double minScore;
    private Double maxScore;
    private Double minGradingScore;
    private Double maxGradingScore;
    private Double minTeachingScore;
    private Double maxTeachingScore;
    private Double minWorkloadScore;
    private Double maxWorkloadScore;
    private Integer minReviewCount;
    private List<Long> tagIds;
    private String tagMatchMode = "OR"; // OR 或 AND
    private String sortBy = "reviewCount";
    private String sortOrder = "desc";
    private Integer difficultyLevel;
    private Integer gradingLevel;
    private Integer page = 1;
    private Integer size = 10;
}
