package com.bjtu.review.controller;

import com.bjtu.review.common.Result;
import com.bjtu.review.dto.ReviewRequest;
import com.bjtu.review.service.ReviewService;
import com.bjtu.review.vo.ReviewVO;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
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
    public Result<List<ReviewVO>> getReviewsByCourse(@PathVariable Long courseId,
                                                     @RequestParam(required = false) Long courseInstanceId,
                                                     @RequestParam(required = false) String sortBy,
                                                     @RequestParam(required = false) List<Long> tagIds) {
        List<ReviewVO> reviews = reviewService.getReviewsByCourseId(courseId, courseInstanceId, sortBy, tagIds);
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
    public Result<List<ReviewVO>> getReviewsByInstance(@PathVariable Long instanceId,
                                                       @RequestParam(required = false) String sortBy,
                                                       @RequestParam(required = false) List<Long> tagIds) {
        return Result.ok(reviewService.getReviewsByCourseInstanceId(instanceId, sortBy, tagIds));
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
    public Result<?> publishReview(Authentication auth, @Valid @RequestBody ReviewRequest request) {
        Long studentId = (Long) auth.getPrincipal();
        reviewService.publishReview(studentId, request);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<?> editReview(Authentication auth, @PathVariable Long id,
                                 @Valid @RequestBody ReviewRequest request) {
        Long studentId = (Long) auth.getPrincipal();
        reviewService.editReview(studentId, id, request);
        return Result.ok();
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
}
