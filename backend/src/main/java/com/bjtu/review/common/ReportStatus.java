package com.bjtu.review.common;

import lombok.Getter;

@Getter
public enum ReportStatus {
    PENDING("待处理"),
    RESOLVED("已处理"),
    DISMISSED("已驳回");

    private final String description;

    ReportStatus(String description) {
        this.description = description;
    }
}
