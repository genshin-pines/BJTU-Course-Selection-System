package com.bjtu.review.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("voter_record")
public class VoterRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String anonymousKey;
    private String displayName;
    private String scopeType;
    private Long courseId;
    private Long teacherId;
    private Long courseInstanceId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
