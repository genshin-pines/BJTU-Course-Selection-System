package com.bjtu.review.service;

import com.bjtu.review.dto.ReviewRequest;
import com.bjtu.review.vo.ReviewVO;
import com.bjtu.review.vo.VoteResultVO;

import java.util.List;

public interface ReviewService {
    List<ReviewVO> getReviewsByCourseId(Long courseId);
    List<ReviewVO> getReviewsByCourseId(Long courseId, Long courseInstanceId);
    List<ReviewVO> getReviewsByCourseId(Long courseId, Long courseInstanceId, String sortBy);
    List<ReviewVO> getReviewsByCourseId(Long courseId, Long courseInstanceId, String sortBy, List<Long> tagIds);
    List<ReviewVO> getReviewsByCourseInstanceId(Long courseInstanceId);
    List<ReviewVO> getReviewsByCourseInstanceId(Long courseInstanceId, String sortBy);
    List<ReviewVO> getReviewsByCourseInstanceId(Long courseInstanceId, String sortBy, List<Long> tagIds);
    void publishReview(Long studentId, ReviewRequest request);
    void editReview(Long studentId, Long reviewId, ReviewRequest request);
    void deleteReview(Long studentId, Long reviewId);
    VoteResultVO toggleLikeReview(Long studentId, Long reviewId);
    VoteResultVO toggleDownvoteReview(Long studentId, Long reviewId);
    List<ReviewVO> getPendingReviews();
    void approveReview(Long adminId, Long reviewId, String reason);
    void rejectReview(Long adminId, Long reviewId, String reason);
    void adminDeleteReview(Long adminId, Long reviewId, String reason);
    List<Long> getLikedReviewIds(Long studentId, Long courseId);
    List<Long> getLikedReviewIds(Long studentId, Long courseId, Long courseInstanceId);
    List<Long> getLikedReviewIdsByInstance(Long studentId, Long courseInstanceId);
    List<Long> getDownvotedReviewIds(Long studentId, Long courseId, Long courseInstanceId);
    List<Long> getDownvotedReviewIdsByInstance(Long studentId, Long courseInstanceId);
}
