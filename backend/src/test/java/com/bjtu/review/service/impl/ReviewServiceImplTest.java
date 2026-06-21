package com.bjtu.review.service.impl;

import com.bjtu.review.common.ReviewStatus;
import com.bjtu.review.dto.ReviewRequest;
import com.bjtu.review.entity.CourseInstance;
import com.bjtu.review.entity.Review;
import com.bjtu.review.entity.ReviewVote;
import com.bjtu.review.entity.VoterRecord;
import com.bjtu.review.mapper.AuditLogMapper;
import com.bjtu.review.mapper.CourseInstanceMapper;
import com.bjtu.review.mapper.ReviewExamExpMapper;
import com.bjtu.review.mapper.ReviewMapper;
import com.bjtu.review.mapper.ReviewTagMapper;
import com.bjtu.review.mapper.ReviewVoteMapper;
import com.bjtu.review.mapper.TeacherMapper;
import com.bjtu.review.service.AnonymityService;
import com.bjtu.review.utils.SensitiveWordFilter;
import com.bjtu.review.vo.ReviewPublishResult;
import com.bjtu.review.vo.VoteResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewMapper reviewMapper;
    @Mock
    private ReviewTagMapper reviewTagMapper;
    @Mock
    private ReviewVoteMapper reviewVoteMapper;
    @Mock
    private TeacherMapper teacherMapper;
    @Mock
    private AuditLogMapper auditLogMapper;
    @Mock
    private AnonymityService anonymityService;
    @Mock
    private CourseInstanceMapper courseInstanceMapper;
    @Mock
    private ReviewExamExpMapper reviewExamExpMapper;

    private ReviewServiceImpl reviewService;
    private SensitiveWordFilter sensitiveWordFilter;

    @BeforeEach
    void setUp() {
        sensitiveWordFilter = new SensitiveWordFilter(new org.springframework.core.io.DefaultResourceLoader());
        sensitiveWordFilter.initFromWords(List.of("badword"));
        reviewService = new ReviewServiceImpl(
                reviewMapper, reviewTagMapper, reviewVoteMapper,
                teacherMapper, sensitiveWordFilter,
                auditLogMapper, anonymityService, courseInstanceMapper, reviewExamExpMapper);
    }

    @Test
    void publishReviewShouldCreatePublishedReviewWithVoterRecord() {
        ReviewRequest request = buildRequest();
        request.setCourseInstanceId(88L);
        when(courseInstanceMapper.selectById(88L)).thenReturn(courseInstance());
        when(anonymityService.getOrCreateCourseReviewRecord(1L, 10L, 20L, 88L)).thenReturn(voterRecord(88L));
        when(reviewMapper.selectCount(any())).thenReturn(0L);
        when(reviewMapper.selectList(any())).thenReturn(List.of());
        doAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setId(99L);
            return 1;
        }).when(reviewMapper).insert(any(Review.class));

        reviewService.publishReview(1L, request);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewMapper).insert(captor.capture());
        Review saved = captor.getValue();
        assertEquals(ReviewStatus.PUBLISHED.name(), saved.getStatus());
        assertEquals(30L, saved.getVoterRecordId());
        assertEquals("anon-key", saved.getAnonymousUserKey());
        assertEquals(88L, saved.getCourseInstanceId());
        assertEquals(4, saved.getOverallScore());
        verify(reviewExamExpMapper).upsertBasicExperience(99L, "closed book", "study tips", "chapter 1-3", true);
        verify(reviewTagMapper).insertTag(99L, 1L);
        verify(reviewTagMapper).insertTag(99L, 2L);
        verify(courseInstanceMapper).updateScores(88L);
        verify(teacherMapper).updateAvgScore(20L);
    }

    @Test
    void publishReviewShouldAutoHideWhenSensitiveWordDetected() {
        ReviewRequest request = buildRequest();
        request.setContent("contains badword here");
        when(courseInstanceMapper.selectByLegacyCourseId(10L)).thenReturn(courseInstance());
        when(anonymityService.getOrCreateCourseReviewRecord(1L, 10L, 20L, 88L)).thenReturn(voterRecord(88L));
        when(reviewMapper.selectCount(any())).thenReturn(0L);
        when(reviewMapper.selectList(any())).thenReturn(List.of());
        doAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setId(100L);
            return 1;
        }).when(reviewMapper).insert(any(Review.class));

        ReviewPublishResult result = reviewService.publishReview(1L, request);

        assertTrue(result.isHidden());
        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewMapper).insert(captor.capture());
        Review saved = captor.getValue();
        assertEquals(ReviewStatus.HIDDEN.name(), saved.getStatus());
        assertEquals("违禁词自动拦截", saved.getHideReason());
        verifyNoInteractions(teacherMapper);
    }

    @Test
    void publishReviewShouldAllowInstanceOnlyRequest() {
        ReviewRequest request = buildRequest();
        request.setCourseId(null);
        request.setTeacherId(null);
        request.setCourseInstanceId(88L);
        when(courseInstanceMapper.selectById(88L)).thenReturn(courseInstance());
        when(anonymityService.getOrCreateCourseReviewRecord(1L, 10L, 20L, 88L)).thenReturn(voterRecord(88L));
        when(reviewMapper.selectCount(any())).thenReturn(0L);
        when(reviewMapper.selectList(any())).thenReturn(List.of());

        reviewService.publishReview(1L, request);

        verify(reviewMapper).insert(argThat(review ->
                review.getCourseId().equals(10L)
                        && review.getTeacherId().equals(20L)
                        && review.getCourseInstanceId().equals(88L)
                        && review.getVoterRecordId().equals(30L)));
    }

    @Test
    void publishReviewShouldRejectDuplicateActiveReviewForSameAnonymousRecord() {
        ReviewRequest request = buildRequest();
        request.setCourseInstanceId(88L);
        when(courseInstanceMapper.selectById(88L)).thenReturn(courseInstance());
        when(anonymityService.getOrCreateCourseReviewRecord(1L, 10L, 20L, 88L)).thenReturn(voterRecord(88L));
        when(reviewMapper.selectCount(any())).thenReturn(1L);

        assertThrows(RuntimeException.class, () -> reviewService.publishReview(1L, request));

        verify(reviewMapper, never()).insert(any(Review.class));
    }

    @Test
    void approveReviewShouldMarkApprovedAndRefreshAggregates() {
        Review review = new Review();
        review.setId(8L);
        review.setCourseId(10L);
        review.setCourseInstanceId(88L);
        review.setTeacherId(20L);
        review.setStatus(ReviewStatus.PUBLISHED.name());
        when(reviewMapper.selectById(8L)).thenReturn(review);

        reviewService.approveReview(7L, 8L, null);

        assertEquals(ReviewStatus.APPROVED.name(), review.getStatus());
        verify(reviewMapper).updateById(review);
        verify(auditLogMapper).insert(argThat(log ->
                log.getAdminId().equals(7L)
                        && log.getReviewId().equals(8L)
                        && "APPROVE_REVIEW".equals(log.getOperateType())
                        && "审核通过".equals(log.getReason())));
        verify(courseInstanceMapper).updateScores(88L);
        verify(teacherMapper).updateAvgScore(20L);
    }

    @Test
    void editReviewShouldUseVoterRecordOwnership() {
        Review existing = new Review();
        existing.setId(3L);
        existing.setVoterRecordId(30L);
        existing.setCourseId(11L);
        existing.setCourseInstanceId(89L);
        existing.setTeacherId(22L);
        existing.setStatus(ReviewStatus.PUBLISHED.name());
        when(reviewMapper.selectById(3L)).thenReturn(existing);
        when(anonymityService.isOwner(1L, 30L)).thenReturn(true);

        ReviewPublishResult result = reviewService.editReview(1L, 3L, buildRequest());

        assertEquals(ReviewStatus.PUBLISHED.name(), existing.getStatus());
        assertEquals(false, result.isHidden());
        assertEquals("评价已更新，将重新进入审核", result.getMessage());
        verify(reviewMapper).updateById(existing);
        verify(reviewExamExpMapper).upsertBasicExperience(3L, "closed book", "study tips", "chapter 1-3", true);
        verify(reviewTagMapper).deleteByReviewId(3L);
        verify(courseInstanceMapper).updateScores(89L);
        verify(teacherMapper).updateAvgScore(22L);
    }

    @Test
    void editReviewShouldNotFallbackToLegacyStudentId() {
        Review existing = new Review();
        existing.setId(3L);
        existing.setStatus(ReviewStatus.PUBLISHED.name());
        when(reviewMapper.selectById(3L)).thenReturn(existing);

        assertThrows(RuntimeException.class, () -> reviewService.editReview(1L, 3L, buildRequest()));

        verify(reviewMapper, never()).updateById(any(Review.class));
    }

    @Test
    void toggleLikeReviewShouldCreateAnonymousVoteAndIncrementLikeCount() {
        Review review = publishedReview();
        Review updated = publishedReview();
        updated.setLikeCount(1);
        when(reviewMapper.selectById(5L)).thenReturn(review, updated);
        when(anonymityService.getOrCreateCourseReviewRecord(1L, 10L, 20L, 0L)).thenReturn(voterRecord(0L));
        ReviewVote finalVote = new ReviewVote();
        finalVote.setVoteType("UPVOTE");
        when(reviewVoteMapper.selectOne(any())).thenReturn(null, finalVote);

        VoteResultVO result = reviewService.toggleLikeReview(1L, 5L);

        assertEquals(1, result.getLikeCount());
        assertEquals(true, result.getLiked());
        assertEquals(false, result.getDownvoted());
        assertEquals("UPVOTE", result.getVoteType());
        verify(reviewVoteMapper).insert(argThat(vote ->
                vote.getReviewId().equals(5L)
                        && vote.getVoterRecordId().equals(30L)
                        && "UPVOTE".equals(vote.getVoteType())));
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
        existingVote.setVoteType("UPVOTE");
        when(reviewMapper.selectById(5L)).thenReturn(review, updated);
        when(anonymityService.getOrCreateCourseReviewRecord(1L, 10L, 20L, 0L)).thenReturn(voterRecord(0L));
        when(reviewVoteMapper.selectOne(any())).thenReturn(existingVote).thenReturn(null);

        VoteResultVO result = reviewService.toggleLikeReview(1L, 5L);

        assertEquals(0, result.getLikeCount());
        assertEquals(false, result.getLiked());
        assertEquals(false, result.getDownvoted());
        assertEquals(null, result.getVoteType());
        verify(reviewVoteMapper).deleteById(9L);
        verify(reviewMapper).decrementLike(5L);
    }

    @Test
    void toggleDownvoteReviewShouldSwitchExistingUpvote() {
        Review review = publishedReview();
        review.setLikeCount(1);
        Review updated = publishedReview();
        updated.setLikeCount(0);
        updated.setDownvoteCount(1);
        ReviewVote existingVote = new ReviewVote();
        existingVote.setId(9L);
        existingVote.setVoteType("UPVOTE");
        when(reviewMapper.selectById(5L)).thenReturn(review, updated);
        when(anonymityService.getOrCreateCourseReviewRecord(1L, 10L, 20L, 0L)).thenReturn(voterRecord(0L));
        when(reviewVoteMapper.selectOne(any())).thenReturn(existingVote, existingVote);

        VoteResultVO result = reviewService.toggleDownvoteReview(1L, 5L);

        assertEquals(0, result.getLikeCount());
        assertEquals(1, result.getDownvoteCount());
        assertEquals(false, result.getLiked());
        assertEquals(true, result.getDownvoted());
        assertEquals("DOWNVOTE", result.getVoteType());
        verify(reviewVoteMapper).updateById(argThat(vote ->
                vote.getId().equals(9L) && "DOWNVOTE".equals(vote.getVoteType())));
        verify(reviewMapper).decrementLike(5L);
        verify(reviewMapper).incrementDownvote(5L);
    }

    @Test
    void toggleVoteReviewShouldRejectOwnerVote() {
        Review review = publishedReview();
        review.setVoterRecordId(30L);
        when(reviewMapper.selectById(5L)).thenReturn(review);
        when(anonymityService.isOwner(1L, 30L)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> reviewService.toggleLikeReview(1L, 5L));

        verify(anonymityService, never()).getOrCreateCourseReviewRecord(any(), any(), any(), any());
        verify(reviewVoteMapper, never()).insert(any(ReviewVote.class));
        verify(reviewMapper, never()).incrementLike(5L);
        verify(reviewMapper, never()).incrementDownvote(5L);
    }

    private ReviewRequest buildRequest() {
        ReviewRequest request = new ReviewRequest();
        request.setCourseId(10L);
        request.setTeacherId(20L);
        request.setGradingScore(5);
        request.setTeachingScore(4);
        request.setWorkloadScore(4);
        request.setContent("normal content");
        request.setStudyTips("study tips");
        request.setExamType("closed book");
        request.setKeyChapters("chapter 1-3");
        request.setCheatSheetAllowed(true);
        request.setTags(List.of(1L, 2L));
        return request;
    }

    private CourseInstance courseInstance() {
        CourseInstance instance = new CourseInstance();
        instance.setId(88L);
        instance.setCourseBaseId(10L);
        instance.setTeacherId(20L);
        return instance;
    }

    private VoterRecord voterRecord(Long courseInstanceId) {
        VoterRecord record = new VoterRecord();
        record.setId(30L);
        record.setStudentId(1L);
        record.setAnonymousKey("anon-key");
        record.setDisplayName("anonymous");
        record.setCourseId(10L);
        record.setTeacherId(20L);
        record.setCourseInstanceId(courseInstanceId);
        return record;
    }

    private Review publishedReview() {
        Review review = new Review();
        review.setId(5L);
        review.setCourseId(10L);
        review.setTeacherId(20L);
        review.setCourseInstanceId(0L);
        review.setLikeCount(0);
        review.setDownvoteCount(0);
        review.setStatus(ReviewStatus.PUBLISHED.name());
        return review;
    }
}
