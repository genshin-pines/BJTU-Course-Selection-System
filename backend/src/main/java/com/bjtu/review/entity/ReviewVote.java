package com.bjtu.review.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("review_vote")
public class ReviewVote {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reviewId;
    private Long studentId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
