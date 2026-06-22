package com.bjtu.review.service;

import com.bjtu.review.dto.ReviewRequest;
import com.bjtu.review.service.PageResult;
import com.bjtu.review.vo.ReviewPublishResult;
import com.bjtu.review.vo.ReviewVO;
import com.bjtu.review.vo.VoteResultVO;

import java.time.LocalDateTime;
import java.util.List;

public interface ReviewService {
    List<ReviewVO> getReviewsByCourseId(Long courseId);
    List<ReviewVO> getReviewsByCourseId(Long courseId, Long courseInstanceId);
    List<ReviewVO> getReviewsByCourseId(Long courseId, Long courseInstanceId, String sortBy);
    List<ReviewVO> getReviewsByCourseId(Long courseId, Long courseInstanceId, String sortBy, List<Long> tagIds);
    List<ReviewVO> getReviewsByCourseId(Long courseId, Long courseInstanceId, String sortBy, List<Long> tagIds, Long studentId);
    List<ReviewVO> getReviewsByCourseInstanceId(Long courseInstanceId);
    List<ReviewVO> getReviewsByCourseInstanceId(Long courseInstanceId, String sortBy);
    List<ReviewVO> getReviewsByCourseInstanceId(Long courseInstanceId, String sortBy, List<Long> tagIds);
    List<ReviewVO> getReviewsByCourseInstanceId(Long courseInstanceId, String sortBy, List<Long> tagIds, Long studentId);
    ReviewPublishResult publishReview(Long studentId, ReviewRequest request);
    ReviewPublishResult editReview(Long studentId, Long reviewId, ReviewRequest request);
    ReviewVO getStudentReview(Long studentId, Long reviewId);
    ReviewVO getMyReviewForCourse(Long studentId, Long courseId, Long courseInstanceId, Long teacherId);
    List<ReviewVO> getMyAllReviews(Long studentId);
    void deleteReview(Long studentId, Long reviewId);
    VoteResultVO toggleLikeReview(Long studentId, Long reviewId);
    VoteResultVO toggleDownvoteReview(Long studentId, Long reviewId);
    List<ReviewVO> getPendingReviews();
    PageResult<ReviewVO> getAdminReviews(String role,
                                         String scopedDepartment,
                                         String status,
                                         String courseName,
                                         String teacherName,
                                         String department,
                                         LocalDateTime startTime,
                                         LocalDateTime endTime,
                                         int page,
                                         int pageSize);
    void approveReview(Long adminId, Long reviewId, String reason);
    void rejectReview(Long adminId, Long reviewId, String reason);
    void adminDeleteReview(Long adminId, Long reviewId, String reason);
    List<Long> getLikedReviewIds(Long studentId, Long courseId);
    List<Long> getLikedReviewIds(Long studentId, Long courseId, Long courseInstanceId);
    List<Long> getLikedReviewIdsByInstance(Long studentId, Long courseInstanceId);
    List<Long> getDownvotedReviewIds(Long studentId, Long courseId, Long courseInstanceId);
    List<Long> getDownvotedReviewIdsByInstance(Long studentId, Long courseInstanceId);
}
