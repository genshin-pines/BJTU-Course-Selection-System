package com.bjtu.review.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("student")
public class Student {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String studentNo;
    private String name;
    private String anonymousId;
    private String password;
    private String major;
    private String grade;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
