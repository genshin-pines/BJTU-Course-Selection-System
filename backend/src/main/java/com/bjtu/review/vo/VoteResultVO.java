package com.bjtu.review.vo;

import lombok.Data;

@Data
public class VoteResultVO {
    private Integer likeCount;
    private Integer downvoteCount;
    private Boolean liked;
    private Boolean downvoted;
    private String voteType;

    public VoteResultVO(Integer likeCount, Boolean liked) {
        this(likeCount, 0, liked, false, liked ? "UPVOTE" : null);
    }

    public VoteResultVO(Integer likeCount, Integer downvoteCount, Boolean liked, Boolean downvoted, String voteType) {
        this.likeCount = likeCount;
        this.downvoteCount = downvoteCount;
        this.liked = liked;
        this.downvoted = downvoted;
        this.voteType = voteType;
    }
}
