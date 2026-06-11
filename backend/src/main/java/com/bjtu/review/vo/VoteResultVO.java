package com.bjtu.review.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VoteResultVO {
    private Integer likeCount;
    private Boolean liked;
}
