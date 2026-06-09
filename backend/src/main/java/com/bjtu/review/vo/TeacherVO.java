package com.bjtu.review.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TeacherVO {
    private Long id;
    private String teacherName;
    private String department;
    private Double avgScore;
    private Double avgTeachingScore;
    private Double avgWorkloadScore;
    private Integer courseCount;
    private Integer reviewCount;
}
