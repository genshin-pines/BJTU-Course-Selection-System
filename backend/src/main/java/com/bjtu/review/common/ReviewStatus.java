package com.bjtu.review.common;

import lombok.Getter;

import java.util.Set;

@Getter
public enum ReviewStatus {
    PENDING_AUDIT("待机审"),
    PENDING_MANUAL("待人工复核"),
    PUBLISHED("已发布"),
    HIDDEN("违规隐藏"),
    ARCHIVED("学期归档"),
    // Legacy statuses kept for backward compatibility with old seeded data.
    PENDING("待审核(旧)"),
    APPROVED("已通过(旧)"),
    REJECTED("已拒绝(旧)");

    private final String description;

    ReviewStatus(String description) {
        this.description = description;
    }

    public static Set<String> visibleStatuses() {
        return Set.of(PUBLISHED.name(), APPROVED.name());
    }

    public static Set<String> pendingStatuses() {
        return Set.of(PENDING_AUDIT.name(), PENDING_MANUAL.name(), PENDING.name());
    }
}
