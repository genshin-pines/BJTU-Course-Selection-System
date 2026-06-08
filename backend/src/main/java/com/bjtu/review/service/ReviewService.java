package com.bjtu.review.service;

import com.bjtu.review.dto.ReviewRequest;
import com.bjtu.review.vo.ReviewVO;

import java.util.List;

public interface ReviewService {
    List<ReviewVO> getReviewsByCourseId(Long courseId);
    void publishReview(Long studentId, ReviewRequest request);
    void editReview(Long studentId, Long reviewId, ReviewRequest request);
    void deleteReview(Long studentId, Long reviewId);
    void likeReview(Long reviewId);
    List<ReviewVO> getPendingReviews();
    void approveReview(Long reviewId);
    void rejectReview(Long reviewId);
    void adminDeleteReview(Long reviewId);
}
