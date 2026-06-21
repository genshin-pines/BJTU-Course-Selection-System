package com.bjtu.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("course_instance")
public class CourseInstance {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseBaseId;
    private Long teacherId;
    private Double avgScore;
    private Double gradingScore;
    private Double avgTeachingScore;
    private Double avgWorkloadScore;
    private Integer reviewCount;
}
