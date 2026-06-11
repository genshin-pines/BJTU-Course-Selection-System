package com.bjtu.review.service.impl;

import com.bjtu.review.common.ReviewStatus;
import com.bjtu.review.dto.ReviewRequest;
import com.bjtu.review.entity.Review;
import com.bjtu.review.entity.ReviewVote;
import com.bjtu.review.mapper.AuditLogMapper;
import com.bjtu.review.mapper.CourseMapper;
import com.bjtu.review.mapper.ReviewMapper;
import com.bjtu.review.mapper.ReviewTagMapper;
import com.bjtu.review.mapper.ReviewVoteMapper;
import com.bjtu.review.mapper.TeacherMapper;
import com.bjtu.review.utils.SensitiveWordFilter;
import com.bjtu.review.vo.VoteResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewMapper reviewMapper;
    @Mock
    private ReviewTagMapper reviewTagMapper;
    @Mock
    private ReviewVoteMapper reviewVoteMapper;
    @Mock
    private CourseMapper courseMapper;
    @Mock
    private TeacherMapper teacherMapper;
    @Mock
    private AuditLogMapper auditLogMapper;
    private ReviewServiceImpl reviewService;
    private SensitiveWordFilter sensitiveWordFilter;

    @BeforeEach
    void setUp() {
        sensitiveWordFilter = new SensitiveWordFilter();
        ReflectionTestUtils.setField(sensitiveWordFilter, "sensitiveWordsConfig", "脏话,垃圾老师,骗子");
        sensitiveWordFilter.init();
        reviewService = new ReviewServiceImpl(
                reviewMapper, reviewTagMapper, reviewVoteMapper,
                courseMapper, teacherMapper, sensitiveWordFilter, auditLogMapper);
    }

    @Test
    void publishReviewShouldCreatePendingAuditReviewWithoutRefreshingAggregates() {
        ReviewRequest request = buildRequest();
        when(reviewMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setId(99L);
            return 1;
        }).when(reviewMapper).insert(any(Review.class));

        reviewService.publishReview(1L, request);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewMapper).insert(captor.capture());
        Review saved = captor.getValue();
        assertEquals(ReviewStatus.PENDING_AUDIT.name(), saved.getStatus());
        assertEquals(4, saved.getOverallScore());
        verify(reviewTagMapper).insertTag(99L, 1L);
        verify(reviewTagMapper).insertTag(99L, 2L);
        verifyNoInteractions(courseMapper, teacherMapper);
    }

    @Test
    void publishReviewShouldRejectDuplicateActiveReviewForSameCourseAndTeacher() {
        ReviewRequest request = buildRequest();
        when(reviewMapper.selectCount(any())).thenReturn(1L);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reviewService.publishReview(1L, request));

        assertEquals("您已经评价过该课程，请勿重复提交", ex.getMessage());
        verify(reviewMapper, never()).insert(any(Review.class));
    }

    @Test
    void approveReviewShouldPublishAndRefreshAggregates() {
        Review review = new Review();
        review.setId(8L);
        review.setCourseId(10L);
        review.setTeacherId(20L);
        review.setStatus(ReviewStatus.PENDING_AUDIT.name());
        when(reviewMapper.selectById(8L)).thenReturn(review);

        reviewService.approveReview(7L, 8L, "内容合规");

        assertEquals(ReviewStatus.PUBLISHED.name(), review.getStatus());
        verify(reviewMapper).updateById(review);
        verify(auditLogMapper).insert(argThat(log ->
                log.getAdminId().equals(7L)
                        && log.getReviewId().equals(8L)
                        && "APPROVE_REVIEW".equals(log.getOperateType())
                        && "内容合规".equals(log.getReason())));
        verify(courseMapper).updateScores(10L);
        verify(teacherMapper).updateAvgScore(20L);
    }

    @Test
    void editReviewShouldReturnPublishedReviewToPendingAuditAndRefreshAggregates() {
        Review existing = new Review();
        existing.setId(3L);
        existing.setStudentId(1L);
        existing.setCourseId(11L);
        existing.setTeacherId(22L);
        existing.setStatus(ReviewStatus.PUBLISHED.name());
        when(reviewMapper.selectById(3L)).thenReturn(existing);
        ReviewRequest request = buildRequest();

        reviewService.editReview(1L, 3L, request);

        assertEquals(ReviewStatus.PENDING_AUDIT.name(), existing.getStatus());
        verify(reviewMapper).updateById(existing);
        verify(reviewTagMapper).deleteByReviewId(3L);
        verify(courseMapper).updateScores(11L);
        verify(teacherMapper).updateAvgScore(22L);
    }

    @Test
    void toggleLikeReviewShouldCreateVoteAndIncrementLikeCount() {
        Review review = publishedReview();
        Review updated = publishedReview();
        updated.setLikeCount(1);
        when(reviewMapper.selectById(5L)).thenReturn(review, updated);
        when(reviewVoteMapper.selectOne(any())).thenReturn(null);

        VoteResultVO result = reviewService.toggleLikeReview(1L, 5L);

        assertEquals(1, result.getLikeCount());
        assertEquals(true, result.getLiked());
        verify(reviewVoteMapper).insert(argThat(vote ->
                vote.getReviewId().equals(5L) && vote.getStudentId().equals(1L)));
        verify(reviewMapper).incrementLike(5L);
    }

    @Test
    void toggleLikeReviewShouldRemoveExistingVoteAndDecrementLikeCount() {
        Review review = publishedReview();
        review.setLikeCount(1);
        Review updated = publishedReview();
        updated.setLikeCount(0);
        ReviewVote existingVote = new ReviewVote();
        existingVote.setId(9L);
        when(reviewMapper.selectById(5L)).thenReturn(review, updated);
        when(reviewVoteMapper.selectOne(any())).thenReturn(existingVote);

        VoteResultVO result = reviewService.toggleLikeReview(1L, 5L);

        assertEquals(0, result.getLikeCount());
        assertEquals(false, result.getLiked());
        verify(reviewVoteMapper).deleteById(9L);
        verify(reviewMapper).decrementLike(5L);
    }

    private ReviewRequest buildRequest() {
        ReviewRequest request = new ReviewRequest();
        request.setCourseId(10L);
        request.setTeacherId(20L);
        request.setGradingScore(5);
        request.setTeachingScore(4);
        request.setWorkloadScore(4);
        request.setContent("内容正常");
        request.setStudyTips("复习建议");
        request.setExamType("闭卷");
        request.setTags(List.of(1L, 2L));
        return request;
    }

    private Review publishedReview() {
        Review review = new Review();
        review.setId(5L);
        review.setStudentId(2L);
        review.setCourseId(10L);
        review.setTeacherId(20L);
        review.setLikeCount(0);
        review.setStatus(ReviewStatus.PUBLISHED.name());
        return review;
    }
}
