package com.bjtu.review.service;

import com.bjtu.review.dto.ReportRequest;
import com.bjtu.review.vo.ReportVO;

import java.util.List;

public interface ReportService {
    void reportReview(Long studentId, ReportRequest request);
    List<ReportVO> getPendingReports();
    List<ReportVO> getAllReports();
    void resolveReport(Long adminId, Long reportId, String reason);
    void dismissReport(Long adminId, Long reportId, String reason);
}
