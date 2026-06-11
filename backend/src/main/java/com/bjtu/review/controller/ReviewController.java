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
    public Result<List<ReviewVO>> getReviewsByCourse(@PathVariable Long courseId) {
        List<ReviewVO> reviews = reviewService.getReviewsByCourseId(courseId);
        return Result.ok(reviews);
    }

    @GetMapping("/course/{courseId}/liked")
    public Result<List<Long>> getLikedReviewIds(Authentication auth, @PathVariable Long courseId) {
        Long studentId = (Long) auth.getPrincipal();
        return Result.ok(reviewService.getLikedReviewIds(studentId, courseId));
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
}
