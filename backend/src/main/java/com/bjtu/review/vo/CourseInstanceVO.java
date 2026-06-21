package com.bjtu.review.vo;

import lombok.Data;

@Data
public class CourseInstanceVO {
    private Long id;
    private Long courseBaseId;
    private Long teacherId;
    private String teacherName;
    private String courseCode;
    private String courseName;
    private Double avgScore;
    private Double gradingScore;
    private Double avgTeachingScore;
    private Double avgWorkloadScore;
    private Integer reviewCount;
}
