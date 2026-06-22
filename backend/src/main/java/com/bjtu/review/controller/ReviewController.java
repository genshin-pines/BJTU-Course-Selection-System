package com.bjtu.review.controller;

import com.bjtu.review.common.Result;
import com.bjtu.review.dto.ReviewRequest;
import com.bjtu.review.service.ReviewService;
import com.bjtu.review.vo.ReviewPublishResult;
import com.bjtu.review.vo.ReviewVO;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/course/{courseId}")
    public Result<List<ReviewVO>> getReviewsByCourse(Authentication auth,
                                                     @PathVariable Long courseId,
                                                     @RequestParam(required = false) Long courseInstanceId,
                                                     @RequestParam(required = false) String sortBy,
                                                     @RequestParam(required = false) List<Long> tagIds) {
        Long studentId = extractStudentId(auth);
        List<ReviewVO> reviews = reviewService.getReviewsByCourseId(
                courseId, courseInstanceId, sortBy, tagIds, studentId);
        return Result.ok(reviews);
    }

    @GetMapping("/course/{courseId}/liked")
    public Result<List<Long>> getLikedReviewIds(Authentication auth, @PathVariable Long courseId,
                                               @RequestParam(required = false) Long courseInstanceId) {
        Long studentId = (Long) auth.getPrincipal();
        return Result.ok(reviewService.getLikedReviewIds(studentId, courseId, courseInstanceId));
    }

    @GetMapping("/course/{courseId}/downvoted")
    public Result<List<Long>> getDownvotedReviewIds(Authentication auth, @PathVariable Long courseId,
                                                   @RequestParam(required = false) Long courseInstanceId) {
        Long studentId = (Long) auth.getPrincipal();
        return Result.ok(reviewService.getDownvotedReviewIds(studentId, courseId, courseInstanceId));
    }

    @GetMapping("/instance/{instanceId}")
    public Result<List<ReviewVO>> getReviewsByInstance(Authentication auth,
                                                       @PathVariable Long instanceId,
                                                       @RequestParam(required = false) String sortBy,
                                                       @RequestParam(required = false) List<Long> tagIds) {
        Long studentId = extractStudentId(auth);
        return Result.ok(reviewService.getReviewsByCourseInstanceId(instanceId, sortBy, tagIds, studentId));
    }

    @GetMapping("/mine")
    public Result<ReviewVO> getMyReview(Authentication auth,
                                        @RequestParam Long courseId,
                                        @RequestParam(required = false) Long courseInstanceId,
                                        @RequestParam Long teacherId) {
        Long studentId = (Long) auth.getPrincipal();
        ReviewVO review = reviewService.getMyReviewForCourse(studentId, courseId, courseInstanceId, teacherId);
        return Result.ok(review);
    }

    @GetMapping("/{id}/mine")
    public Result<ReviewVO> getMyReviewById(Authentication auth, @PathVariable Long id) {
        Long studentId = (Long) auth.getPrincipal();
        return Result.ok(reviewService.getStudentReview(studentId, id));
    }

    @GetMapping("/mine/all")
    public Result<List<ReviewVO>> getMyAllReviews(Authentication auth) {
        Long studentId = (Long) auth.getPrincipal();
        return Result.ok(reviewService.getMyAllReviews(studentId));
    }

    @GetMapping("/instance/{instanceId}/liked")
    public Result<List<Long>> getLikedReviewIdsByInstance(Authentication auth, @PathVariable Long instanceId) {
        Long studentId = (Long) auth.getPrincipal();
        return Result.ok(reviewService.getLikedReviewIdsByInstance(studentId, instanceId));
    }

    @GetMapping("/instance/{instanceId}/downvoted")
    public Result<List<Long>> getDownvotedReviewIdsByInstance(Authentication auth, @PathVariable Long instanceId) {
        Long studentId = (Long) auth.getPrincipal();
        return Result.ok(reviewService.getDownvotedReviewIdsByInstance(studentId, instanceId));
    }

    @PostMapping
    public Result<ReviewPublishResult> publishReview(Authentication auth, @Valid @RequestBody ReviewRequest request) {
        Long studentId = (Long) auth.getPrincipal();
        ReviewPublishResult result = reviewService.publishReview(studentId, request);
        if (result.isHidden()) {
            return new Result<>(200, result.getMessage(), result);
        }
        return Result.ok(result);
    }

    @PutMapping("/{id}")
    public Result<ReviewPublishResult> editReview(Authentication auth, @PathVariable Long id,
                                 @Valid @RequestBody ReviewRequest request) {
        Long studentId = (Long) auth.getPrincipal();
        ReviewPublishResult result = reviewService.editReview(studentId, id, request);
        if (result.isHidden()) {
            return new Result<>(200, result.getMessage(), result);
        }
        return Result.ok(result);
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteReview(Authentication auth, @PathVariable Long id) {
        Long studentId = (Long) auth.getPrincipal();
        reviewService.deleteReview(studentId, id);
        return Result.ok();
    }

    @PostMapping("/like/{id}")
    public Result<?> likeReview(Authentication auth, @PathVariable Long id) {
        Long studentId = (Long) auth.getPrincipal();
        return Result.ok(reviewService.toggleLikeReview(studentId, id));
    }

    @PostMapping("/downvote/{id}")
    public Result<?> downvoteReview(Authentication auth, @PathVariable Long id) {
        Long studentId = (Long) auth.getPrincipal();
        return Result.ok(reviewService.toggleDownvoteReview(studentId, id));
    }

    private Long extractStudentId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        boolean isStudent = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_STUDENT"::equals);
        if (!isStudent) {
            return null;
        }
        return (Long) auth.getPrincipal();
    }
}
