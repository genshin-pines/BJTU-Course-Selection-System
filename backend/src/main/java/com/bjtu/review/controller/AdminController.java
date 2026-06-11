package com.bjtu.review.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bjtu.review.common.Result;
import com.bjtu.review.dto.AdminOperationRequest;
import com.bjtu.review.entity.AuditLog;
import com.bjtu.review.entity.Tag;
import com.bjtu.review.mapper.AuditLogMapper;
import com.bjtu.review.service.ReportService;
import com.bjtu.review.service.ReviewService;
import com.bjtu.review.service.TagService;
import com.bjtu.review.vo.ReportVO;
import com.bjtu.review.vo.ReviewVO;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ReviewService reviewService;
    private final ReportService reportService;
    private final TagService tagService;
    private final AuditLogMapper auditLogMapper;

    public AdminController(ReviewService reviewService, ReportService reportService,
                           TagService tagService, AuditLogMapper auditLogMapper) {
        this.reviewService = reviewService;
        this.reportService = reportService;
        this.tagService = tagService;
        this.auditLogMapper = auditLogMapper;
    }

    // 评价审核
    @GetMapping("/reviews/pending")
    public Result<List<ReviewVO>> getPendingReviews() {
        return Result.ok(reviewService.getPendingReviews());
    }

    @PutMapping("/reviews/{id}/approve")
    public Result<?> approveReview(Authentication auth, @PathVariable Long id,
                                   @RequestBody(required = false) AdminOperationRequest request) {
        reviewService.approveReview(adminId(auth), id, reason(request));
        return Result.ok();
    }

    @PutMapping("/reviews/{id}/reject")
    public Result<?> rejectReview(Authentication auth, @PathVariable Long id,
                                  @RequestBody(required = false) AdminOperationRequest request) {
        reviewService.rejectReview(adminId(auth), id, reason(request));
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
    public Result<?> resolveReport(Authentication auth, @PathVariable Long id,
                                   @RequestBody(required = false) AdminOperationRequest request) {
        reportService.resolveReport(adminId(auth), id, reason(request));
        return Result.ok();
    }

    @PutMapping("/reports/{id}/dismiss")
    public Result<?> dismissReport(Authentication auth, @PathVariable Long id,
                                   @RequestBody(required = false) AdminOperationRequest request) {
        reportService.dismissReport(adminId(auth), id, reason(request));
        return Result.ok();
    }

    // 删除违规评价
    @DeleteMapping("/reviews/{id}")
    public Result<?> deleteReview(Authentication auth, @PathVariable Long id,
                                  @RequestBody(required = false) AdminOperationRequest request) {
        reviewService.adminDeleteReview(adminId(auth), id, reason(request));
        return Result.ok();
    }

    @GetMapping("/audit-logs")
    public Result<List<AuditLog>> getAuditLogs() {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(AuditLog::getCreateTime);
        wrapper.last("LIMIT 100");
        return Result.ok(auditLogMapper.selectList(wrapper));
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

    private Long adminId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }

    private String reason(AdminOperationRequest request) {
        return request == null ? null : request.getReason();
    }
}
