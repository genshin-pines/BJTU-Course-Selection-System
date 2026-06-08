package com.bjtu.review.common;

import lombok.Getter;

@Getter
public enum ReviewStatus {
    PENDING("待审核"),
    APPROVED("已通过"),
    REJECTED("已拒绝");

    private final String description;

    ReviewStatus(String description) {
        this.description = description;
    }
}
