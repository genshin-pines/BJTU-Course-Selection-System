package com.bjtu.review.service;

import com.bjtu.review.entity.VoterRecord;

public interface AnonymityService {
    VoterRecord getOrCreateCourseReviewRecord(Long studentId, Long courseId, Long teacherId, Long courseInstanceId);

    boolean isOwner(Long studentId, Long voterRecordId);
}
