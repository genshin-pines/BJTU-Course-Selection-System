package com.bjtu.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bjtu.review.common.ReportStatus;
import com.bjtu.review.common.ReviewStatus;
import com.bjtu.review.dto.ReportRequest;
import com.bjtu.review.entity.AuditLog;
import com.bjtu.review.entity.Report;
import com.bjtu.review.entity.Review;
import com.bjtu.review.entity.VoterRecord;
import com.bjtu.review.mapper.AuditLogMapper;
import com.bjtu.review.mapper.CourseInstanceMapper;
import com.bjtu.review.mapper.ReportMapper;
import com.bjtu.review.mapper.ReviewMapper;
import com.bjtu.review.mapper.ReviewTagMapper;
import com.bjtu.review.mapper.TeacherMapper;
import com.bjtu.review.service.AnonymityService;
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
    private final CourseInstanceMapper courseInstanceMapper;
    private final TeacherMapper teacherMapper;
    private final AuditLogMapper auditLogMapper;
    private final AnonymityService anonymityService;

    public ReportServiceImpl(ReportMapper reportMapper, ReviewMapper reviewMapper,
                              ReviewTagMapper reviewTagMapper,
                              CourseInstanceMapper courseInstanceMapper,
                              TeacherMapper teacherMapper, AuditLogMapper auditLogMapper,
                              AnonymityService anonymityService) {
        this.reportMapper = reportMapper;
        this.reviewMapper = reviewMapper;
        this.reviewTagMapper = reviewTagMapper;
        this.courseInstanceMapper = courseInstanceMapper;
        this.teacherMapper = teacherMapper;
        this.auditLogMapper = auditLogMapper;
        this.anonymityService = anonymityService;
    }

    @Override
    public void reportReview(Long studentId, ReportRequest request) {
        Review review = reviewMapper.selectById(request.getReviewId());
        if (review == null) {
            throw new RuntimeException("评价不存在");
        }
        VoterRecord reporterRecord = anonymityService.getOrCreateCourseReviewRecord(
                studentId, review.getCourseId(), review.getTeacherId(), review.getCourseInstanceId());
        if (isSelfReport(reporterRecord, review)) {
            throw new RuntimeException("不能举报自己的评价");
        }
        if (!ReviewStatus.PUBLISHED.name().equals(review.getStatus()) && !"APPROVED".equals(review.getStatus())) {
            throw new RuntimeException("只能举报已发布评价");
        }

        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Report::getReviewId, request.getReviewId())
               .eq(Report::getReporterRecordId, reporterRecord.getId())
               .eq(Report::getStatus, ReportStatus.PENDING.name());
        if (reportMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("您已举报过该评价，请等待处理");
        }

        Report report = new Report();
        report.setReviewId(request.getReviewId());
        report.setReporterRecordId(reporterRecord.getId());
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
    public void resolveReport(Long adminId, Long reportId, String reason) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new RuntimeException("举报不存在");
        }
        report.setStatus(ReportStatus.RESOLVED.name());
        reportMapper.updateById(report);

        Review review = reviewMapper.selectById(report.getReviewId());
        if (review != null) {
            reviewTagMapper.deleteByReviewId(report.getReviewId());
            reviewMapper.deleteById(report.getReviewId());
            if (review.getCourseInstanceId() != null && review.getCourseInstanceId() > 0) {
                courseInstanceMapper.updateScores(review.getCourseInstanceId());
            }
            teacherMapper.updateAvgScore(review.getTeacherId());
        }
        writeAuditLog(adminId, report.getReviewId(), reportId, "RESOLVE_REPORT", defaultReason(reason, "采纳举报"));
    }

    @Override
    public void dismissReport(Long adminId, Long reportId, String reason) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new RuntimeException("举报不存在");
        }
        report.setStatus(ReportStatus.DISMISSED.name());
        reportMapper.updateById(report);
        writeAuditLog(adminId, report.getReviewId(), reportId, "DISMISS_REPORT", defaultReason(reason, "驳回举报"));
    }

    private void writeAuditLog(Long adminId, Long reviewId, Long reportId, String operateType, String reason) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAdminId(adminId);
        auditLog.setReviewId(reviewId);
        auditLog.setReportId(reportId);
        auditLog.setOperateType(operateType);
        auditLog.setReason(reason);
        auditLogMapper.insert(auditLog);
    }

    private String defaultReason(String reason, String fallback) {
        if (reason == null || reason.isBlank()) {
            return fallback;
        }
        return reason.trim();
    }

    private boolean isSelfReport(VoterRecord reporterRecord, Review review) {
        return review.getVoterRecordId() != null && review.getVoterRecordId().equals(reporterRecord.getId());
    }
}
