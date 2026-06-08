package com.bjtu.review.controller;

import com.bjtu.review.common.Result;
import com.bjtu.review.entity.Tag;
import com.bjtu.review.service.ReportService;
import com.bjtu.review.service.ReviewService;
import com.bjtu.review.service.TagService;
import com.bjtu.review.vo.ReportVO;
import com.bjtu.review.vo.ReviewVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ReviewService reviewService;
    private final ReportService reportService;
    private final TagService tagService;

    public AdminController(ReviewService reviewService, ReportService reportService,
                           TagService tagService) {
        this.reviewService = reviewService;
        this.reportService = reportService;
        this.tagService = tagService;
    }

    // 评价审核
    @GetMapping("/reviews/pending")
    public Result<List<ReviewVO>> getPendingReviews() {
        return Result.ok(reviewService.getPendingReviews());
    }

    @PutMapping("/reviews/{id}/approve")
    public Result<?> approveReview(@PathVariable Long id) {
        reviewService.approveReview(id);
        return Result.ok();
    }

    @PutMapping("/reviews/{id}/reject")
    public Result<?> rejectReview(@PathVariable Long id) {
        reviewService.rejectReview(id);
        return Result.ok();
    }

    // 举报管理
    @GetMapping("/reports/pending")
    public Result<List<ReportVO>> getPendingReports() {
        return Result.ok(reportService.getPendingReports());
    }

    @GetMapping("/reports")
    public Result<List<ReportVO>> getAllReports() {
        return Result.ok(reportService.getAllReports());
    }

    @PutMapping("/reports/{id}/resolve")
    public Result<?> resolveReport(@PathVariable Long id) {
        reportService.resolveReport(id);
        return Result.ok();
    }

    @PutMapping("/reports/{id}/dismiss")
    public Result<?> dismissReport(@PathVariable Long id) {
        reportService.dismissReport(id);
        return Result.ok();
    }

    // 删除违规评价
    @DeleteMapping("/reviews/{id}")
    public Result<?> deleteReview(@PathVariable Long id) {
        reviewService.adminDeleteReview(id);
        return Result.ok();
    }

    // 标签管理
    @PostMapping("/tags")
    public Result<Tag> createTag(@RequestParam String tagName) {
        return Result.ok(tagService.createTag(tagName));
    }

    @DeleteMapping("/tags/{id}")
    public Result<?> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return Result.ok();
    }
}
