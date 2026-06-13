package com.bjtu.review.controller;

import com.bjtu.review.common.Result;
import com.bjtu.review.dto.ReportRequest;
import com.bjtu.review.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public Result<?> reportReview(Authentication auth, @Valid @RequestBody ReportRequest request) {
        Long studentId = (Long) auth.getPrincipal();
        reportService.reportReview(studentId, request);
        return Result.ok();
    }
}
