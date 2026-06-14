package com.bjtu.review.controller;

import com.bjtu.review.common.Result;
import com.bjtu.review.dto.AdminOperationRequest;
import com.bjtu.review.entity.Admin;
import com.bjtu.review.entity.Tag;
import com.bjtu.review.mapper.AdminMapper;
import com.bjtu.review.mapper.AuditLogMapper;
import com.bjtu.review.service.ReportService;
import com.bjtu.review.service.ReviewService;
import com.bjtu.review.service.TagService;
import com.bjtu.review.vo.AuditLogVO;
import com.bjtu.review.vo.ReportVO;
import com.bjtu.review.vo.ReviewVO;
import org.springframework.security.access.AccessDeniedException;
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
    private final AdminMapper adminMapper;

    public AdminController(ReviewService reviewService, ReportService reportService,
                           TagService tagService, AuditLogMapper auditLogMapper, AdminMapper adminMapper) {
        this.reviewService = reviewService;
        this.reportService = reportService;
        this.tagService = tagService;
        this.auditLogMapper = auditLogMapper;
        this.adminMapper = adminMapper;
    }

    // 评价审核
    @GetMapping("/reviews/pending")
    public Result<List<ReviewVO>> getPendingReviews(Authentication auth) {
        requireContentGovernance(auth);
        return Result.ok(reviewService.getPendingReviews());
    }

    @PutMapping("/reviews/{id}/approve")
    public Result<?> approveReview(Authentication auth, @PathVariable Long id,
                                   @RequestBody(required = false) AdminOperationRequest request) {
        requireContentGovernance(auth);
        reviewService.approveReview(adminId(auth), id, reason(request));
        return Result.ok();
    }

    @PutMapping("/reviews/{id}/reject")
    public Result<?> rejectReview(Authentication auth, @PathVariable Long id,
                                  @RequestBody(required = false) AdminOperationRequest request) {
        requireContentGovernance(auth);
        reviewService.rejectReview(adminId(auth), id, reason(request));
        return Result.ok();
    }

    // 举报管理
    @GetMapping("/reports/pending")
    public Result<List<ReportVO>> getPendingReports(Authentication auth) {
        requireContentGovernance(auth);
        return Result.ok(reportService.getPendingReports());
    }

    @GetMapping("/reports")
    public Result<List<ReportVO>> getAllReports(Authentication auth) {
        requireContentGovernance(auth);
        return Result.ok(reportService.getAllReports());
    }

    @PutMapping("/reports/{id}/resolve")
    public Result<?> resolveReport(Authentication auth, @PathVariable Long id,
                                   @RequestBody(required = false) AdminOperationRequest request) {
        requireContentGovernance(auth);
        reportService.resolveReport(adminId(auth), id, reason(request));
        return Result.ok();
    }

    @PutMapping("/reports/{id}/dismiss")
    public Result<?> dismissReport(Authentication auth, @PathVariable Long id,
                                   @RequestBody(required = false) AdminOperationRequest request) {
        requireContentGovernance(auth);
        reportService.dismissReport(adminId(auth), id, reason(request));
        return Result.ok();
    }

    // 删除违规评价
    @DeleteMapping("/reviews/{id}")
    public Result<?> deleteReview(Authentication auth, @PathVariable Long id,
                                  @RequestBody(required = false) AdminOperationRequest request) {
        requireContentGovernance(auth);
        reviewService.adminDeleteReview(adminId(auth), id, reason(request));
        return Result.ok();
    }

    @GetMapping("/audit-logs")
    public Result<List<AuditLogVO>> getAuditLogs(Authentication auth) {
        requireAnyAdmin(auth);
        return Result.ok(auditLogMapper.selectRecentLogs());
    }

    // 标签管理
    @PostMapping("/tags")
    public Result<Tag> createTag(Authentication auth, @RequestParam String tagName) {
        requireDataMaintenance(auth);
        return Result.ok(tagService.createTag(tagName));
    }

    @DeleteMapping("/tags/{id}")
    public Result<?> deleteTag(Authentication auth, @PathVariable Long id) {
        requireDataMaintenance(auth);
        tagService.deleteTag(id);
        return Result.ok();
    }

    private Long adminId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }

    private String reason(AdminOperationRequest request) {
        return request == null ? null : request.getReason();
    }

    private void requireContentGovernance(Authentication auth) {
        String role = adminRole(auth);
        if (!"SUPER_ADMIN".equals(role) && !"AUDITOR".equals(role)) {
            throw new AccessDeniedException("当前管理员无内容审核权限");
        }
    }

    private void requireDataMaintenance(Authentication auth) {
        String role = adminRole(auth);
        if (!"SUPER_ADMIN".equals(role) && !"DEPT_OP".equals(role)) {
            throw new AccessDeniedException("当前管理员无数据维护权限");
        }
    }

    private void requireAnyAdmin(Authentication auth) {
        adminRole(auth);
    }

    private String adminRole(Authentication auth) {
        Admin admin = adminMapper.selectById(adminId(auth));
        if (admin == null) {
            throw new AccessDeniedException("管理员账号不存在");
        }
        if (admin.getRole() == null || admin.getRole().isBlank()) {
            return "SUPER_ADMIN";
        }
        return admin.getRole();
    }
}
