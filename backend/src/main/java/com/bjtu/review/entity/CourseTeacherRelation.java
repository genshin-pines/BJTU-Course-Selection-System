package com.bjtu.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("course_teacher_relation")
public class CourseTeacherRelation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseBaseId;
    private Long teacherId;
    private String semester;
}
