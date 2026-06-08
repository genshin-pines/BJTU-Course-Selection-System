package com.bjtu.review.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("course")
public class Course {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String courseCode;
    private String courseName;
    private Integer credit;
    private String department;
    private Long teacherId;
    private Double avgScore;
    private Double difficultyScore;
    private Double gradingScore;
    private Integer reviewCount;
}
