package com.bjtu.review.vo;

import lombok.Data;
import java.util.List;

@Data
public class CourseVO {
    private Long id;
    private String courseCode;
    private String courseName;
    private Integer credit;
    private String department;
    private Long teacherId;
    private String teacherName;
    private Double avgScore;
    private Double gradingScore;
    private Double avgTeachingScore;
    private Double avgWorkloadScore;
    private Integer reviewCount;
    private List<String> topTags;
}
