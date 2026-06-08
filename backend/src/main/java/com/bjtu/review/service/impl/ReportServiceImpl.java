package com.bjtu.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bjtu.review.common.ReportStatus;
import com.bjtu.review.dto.ReportRequest;
import com.bjtu.review.entity.Report;
import com.bjtu.review.entity.Review;
import com.bjtu.review.mapper.ReportMapper;
import com.bjtu.review.mapper.ReviewMapper;
import com.bjtu.review.mapper.ReviewTagMapper;
import com.bjtu.review.service.ReportService;
import com.bjtu.review.vo.ReportVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;
    private final ReviewMapper reviewMapper;
    private final ReviewTagMapper reviewTagMapper;

    public ReportServiceImpl(ReportMapper reportMapper, ReviewMapper reviewMapper,
                             ReviewTagMapper reviewTagMapper) {
        this.reportMapper = reportMapper;
        this.reviewMapper = reviewMapper;
        this.reviewTagMapper = reviewTagMapper;
    }

    @Override
    public void reportReview(Long reporterId, ReportRequest request) {
        // 检查是否已举报
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Report::getReviewId, request.getReviewId())
               .eq(Report::getReporterId, reporterId)
               .eq(Report::getStatus, ReportStatus.PENDING.name());
        if (reportMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("您已举报过该评价，请等待处理");
        }

        Report report = new Report();
        report.setReviewId(request.getReviewId());
        report.setReporterId(reporterId);
        report.setReason(request.getReason());
        report.setStatus(ReportStatus.PENDING.name());
        reportMapper.insert(report);
    }

    @Override
    public List<ReportVO> getPendingReports() {
        return reportMapper.selectPendingReports();
    }

    @Override
    public List<ReportVO> getAllReports() {
        return reportMapper.selectAllReports();
    }

    @Override
    @Transactional
    public void resolveReport(Long reportId) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new RuntimeException("举报不存在");
        }
        report.setStatus(ReportStatus.RESOLVED.name());
        reportMapper.updateById(report);

        // 删除被举报的评价
        reviewTagMapper.deleteByReviewId(report.getReviewId());
        reviewMapper.deleteById(report.getReviewId());
    }

    @Override
    public void dismissReport(Long reportId) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new RuntimeException("举报不存在");
        }
        report.setStatus(ReportStatus.DISMISSED.name());
        reportMapper.updateById(report);
    }
}
