package com.bjtu.review.dto;

import lombok.Data;

@Data
public class AdminCourseRequest {
    private String courseCode;
    private String courseName;
    private Integer credit;
    private String department;
}
