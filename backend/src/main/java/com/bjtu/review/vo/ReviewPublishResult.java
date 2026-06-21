package com.bjtu.review.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewPublishResult {
    private Long reviewId;
    private boolean hidden;
    private String message;

    public static ReviewPublishResult published(Long reviewId) {
        return new ReviewPublishResult(reviewId, false, "评价已发布");
    }

    public static ReviewPublishResult autoHidden(Long reviewId) {
        return new ReviewPublishResult(
                reviewId,
                true,
                "评价包含违禁词，已自动隐藏，不会公开显示，请修改后重新提交");
    }

    public static ReviewPublishResult republishedAfterEdit(Long reviewId) {
        return new ReviewPublishResult(reviewId, false, "评价已更新，将重新进入审核");
    }
}
