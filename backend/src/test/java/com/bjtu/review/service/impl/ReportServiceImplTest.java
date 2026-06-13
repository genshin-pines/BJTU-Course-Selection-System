package com.bjtu.review.service.impl;

import com.bjtu.review.common.ReportStatus;
import com.bjtu.review.common.ReviewStatus;
import com.bjtu.review.dto.ReportRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private ReportMapper reportMapper;
    @Mock
    private ReviewMapper reviewMapper;
    @Mock
    private ReviewTagMapper reviewTagMapper;
    @Mock
    private CourseInstanceMapper courseInstanceMapper;
    @Mock
    private TeacherMapper teacherMapper;
    @Mock
    private AuditLogMapper auditLogMapper;
    @Mock
    private AnonymityService anonymityService;

    private ReportServiceImpl reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportServiceImpl(
                reportMapper, reviewMapper, reviewTagMapper,
                courseInstanceMapper, teacherMapper, auditLogMapper, anonymityService);
    }

    @Test
    void resolveReportShouldDeleteReviewAndRefreshAggregates() {
        Report report = new Report();
        report.setId(9L);
        report.setReviewId(5L);
        report.setStatus(ReportStatus.PENDING.name());
        Review review = new Review();
        review.setId(5L);
        review.setCourseId(100L);
        review.setCourseInstanceId(300L);
        review.setTeacherId(200L);
        when(reportMapper.selectById(9L)).thenReturn(report);
        when(reviewMapper.selectById(5L)).thenReturn(review);

        reportService.resolveReport(7L, 9L, "valid report");

        assertEquals(ReportStatus.RESOLVED.name(), report.getStatus());
        verify(reportMapper).updateById(report);
        verify(reviewTagMapper).deleteByReviewId(5L);
        verify(reviewMapper).deleteById(5L);
        verify(courseInstanceMapper).updateScores(300L);
        verify(teacherMapper).updateAvgScore(200L);
        verify(auditLogMapper).insert(argThat(log ->
                log.getAdminId().equals(7L)
                        && log.getReviewId().equals(5L)
                        && log.getReportId().equals(9L)
                        && "RESOLVE_REPORT".equals(log.getOperateType())
                        && "valid report".equals(log.getReason())));
    }

    @Test
    void reportReviewShouldRejectSelfReportByVoterRecord() {
        Review review = publishedReview();
        review.setVoterRecordId(30L);
        when(reviewMapper.selectById(5L)).thenReturn(review);
        when(anonymityService.getOrCreateCourseReviewRecord(1L, 100L, 200L, 0L)).thenReturn(voterRecord(30L));

        assertThrows(RuntimeException.class, () -> reportService.reportReview(1L, reportRequest()));

        verify(reportMapper, never()).insert(any(Report.class));
    }

    @Test
    void reportReviewShouldNotFallbackToLegacyStudentIdForSelfReport() {
        Review review = publishedReview();
        when(reviewMapper.selectById(5L)).thenReturn(review);
        when(anonymityService.getOrCreateCourseReviewRecord(1L, 100L, 200L, 0L)).thenReturn(voterRecord(31L));
        when(reportMapper.selectCount(any())).thenReturn(0L);

        reportService.reportReview(1L, reportRequest());

        verify(reportMapper).insert(argThat(report ->
                report.getReviewId().equals(5L)
                        && report.getReporterRecordId().equals(31L)));
    }

    @Test
    void reportReviewShouldRejectUnpublishedReview() {
        Review review = publishedReview();
        review.setStatus(ReviewStatus.PENDING_AUDIT.name());
        when(reviewMapper.selectById(5L)).thenReturn(review);
        when(anonymityService.getOrCreateCourseReviewRecord(1L, 100L, 200L, 0L)).thenReturn(voterRecord(31L));

        assertThrows(RuntimeException.class, () -> reportService.reportReview(1L, reportRequest()));

        verify(reportMapper, never()).insert(any(Report.class));
    }

    private Review publishedReview() {
        Review review = new Review();
        review.setId(5L);
        review.setCourseId(100L);
        review.setTeacherId(200L);
        review.setCourseInstanceId(0L);
        review.setStatus(ReviewStatus.PUBLISHED.name());
        return review;
    }

    private ReportRequest reportRequest() {
        ReportRequest request = new ReportRequest();
        request.setReviewId(5L);
        request.setReason("invalid content");
        return request;
    }

    private VoterRecord voterRecord(Long id) {
        VoterRecord record = new VoterRecord();
        record.setId(id);
        record.setStudentId(1L);
        record.setCourseId(100L);
        record.setTeacherId(200L);
        record.setCourseInstanceId(0L);
        record.setAnonymousKey("anon-" + id);
        record.setDisplayName("anonymous-" + id);
        return record;
    }
}
