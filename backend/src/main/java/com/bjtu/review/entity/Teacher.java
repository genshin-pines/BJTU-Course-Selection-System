package com.bjtu.review.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("teacher")
public class Teacher {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String teacherName;
    private String department;
    private Double avgScore;
    private Double avgTeachingScore;
    private Double avgWorkloadScore;
}
