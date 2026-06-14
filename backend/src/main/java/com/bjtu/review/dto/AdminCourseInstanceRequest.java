package com.bjtu.review.dto;

import lombok.Data;

@Data
public class AdminCourseInstanceRequest {
    private Long courseBaseId;
    private Long teacherId;
    private String semester;
    private String className;
}
