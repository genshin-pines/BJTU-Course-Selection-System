package com.bjtu.review.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("review_tag")
public class ReviewTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reviewId;
    private Long tagId;
}
