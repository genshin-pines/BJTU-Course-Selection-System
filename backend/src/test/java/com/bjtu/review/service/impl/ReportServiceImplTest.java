package com.bjtu.review.service.impl;

import com.bjtu.review.common.ReportStatus;
import com.bjtu.review.common.ReviewStatus;
import com.bjtu.review.dto.ReportRequest;
import com.bjtu.review.entity.Report;
import com.bjtu.review.entity.Review;
import com.bjtu.review.mapper.AuditLogMapper;
import com.bjtu.review.mapper.CourseMapper;
import com.bjtu.review.mapper.ReportMapper;
import com.bjtu.review.mapper.ReviewMapper;
import com.bjtu.review.mapper.ReviewTagMapper;
import com.bjtu.review.mapper.TeacherMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private ReportMapper reportMapper;
    @Mock
    private ReviewMapper reviewMapper;
    @Mock
    private ReviewTagMapper reviewTagMapper;
    @Mock
    private CourseMapper courseMapper;
    @Mock
    private TeacherMapper teacherMapper;
    @Mock
    private AuditLogMapper auditLogMapper;

    private ReportServiceImpl reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportServiceImpl(
                reportMapper, reviewMapper, reviewTagMapper, courseMapper, teacherMapper, auditLogMapper);
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
        review.setTeacherId(200L);
        when(reportMapper.selectById(9L)).thenReturn(report);
        when(reviewMapper.selectById(5L)).thenReturn(review);

        reportService.resolveReport(7L, 9L, "举报属实");

        assertEquals(ReportStatus.RESOLVED.name(), report.getStatus());
        verify(reportMapper).updateById(report);
        verify(reviewTagMapper).deleteByReviewId(5L);
        verify(reviewMapper).deleteById(5L);
        verify(courseMapper).updateScores(100L);
        verify(teacherMapper).updateAvgScore(200L);
        verify(auditLogMapper).insert(argThat(log ->
                log.getAdminId().equals(7L)
                        && log.getReviewId().equals(5L)
                        && log.getReportId().equals(9L)
                        && "RESOLVE_REPORT".equals(log.getOperateType())
                        && "举报属实".equals(log.getReason())));
    }

    @Test
    void reportReviewShouldRejectSelfReport() {
        Review review = new Review();
        review.setId(5L);
        review.setStudentId(1L);
        review.setStatus(ReviewStatus.PUBLISHED.name());
        when(reviewMapper.selectById(5L)).thenReturn(review);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reportService.reportReview(1L, reportRequest()));

        assertEquals("不能举报自己的评价", ex.getMessage());
        verify(reportMapper, never()).insert(any(Report.class));
    }

    @Test
    void reportReviewShouldRejectUnpublishedReview() {
        Review review = new Review();
        review.setId(5L);
        review.setStudentId(2L);
        review.setStatus(ReviewStatus.PENDING_AUDIT.name());
        when(reviewMapper.selectById(5L)).thenReturn(review);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reportService.reportReview(1L, reportRequest()));

        assertEquals("只能举报已发布评价", ex.getMessage());
        verify(reportMapper, never()).insert(any(Report.class));
    }

    private ReportRequest reportRequest() {
        ReportRequest request = new ReportRequest();
        request.setReviewId(5L);
        request.setReason("内容不实");
        return request;
    }
}
