package com.bjtu.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bjtu.review.common.ReviewStatus;
import com.bjtu.review.dto.ReviewRequest;
import com.bjtu.review.entity.AuditLog;
import com.bjtu.review.entity.Review;
import com.bjtu.review.entity.ReviewVote;
import com.bjtu.review.entity.Tag;
import com.bjtu.review.mapper.AuditLogMapper;
import com.bjtu.review.mapper.CourseMapper;
import com.bjtu.review.mapper.ReviewMapper;
import com.bjtu.review.mapper.ReviewTagMapper;
import com.bjtu.review.mapper.ReviewVoteMapper;
import com.bjtu.review.mapper.TeacherMapper;
import com.bjtu.review.service.ReviewService;
import com.bjtu.review.utils.SensitiveWordFilter;
import com.bjtu.review.vo.ReviewVO;
import com.bjtu.review.vo.TagVO;
import com.bjtu.review.vo.VoteResultVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final ReviewTagMapper reviewTagMapper;
    private final ReviewVoteMapper reviewVoteMapper;
    private final CourseMapper courseMapper;
    private final TeacherMapper teacherMapper;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final AuditLogMapper auditLogMapper;

    public ReviewServiceImpl(ReviewMapper reviewMapper, ReviewTagMapper reviewTagMapper,
                             ReviewVoteMapper reviewVoteMapper,
                             CourseMapper courseMapper, TeacherMapper teacherMapper,
                             SensitiveWordFilter sensitiveWordFilter,
                             AuditLogMapper auditLogMapper) {
        this.reviewMapper = reviewMapper;
        this.reviewTagMapper = reviewTagMapper;
        this.reviewVoteMapper = reviewVoteMapper;
        this.courseMapper = courseMapper;
        this.teacherMapper = teacherMapper;
        this.sensitiveWordFilter = sensitiveWordFilter;
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    public List<ReviewVO> getReviewsByCourseId(Long courseId) {
        List<ReviewVO> reviews = reviewMapper.selectByCourseId(courseId);
        for (ReviewVO review : reviews) {
            List<Tag> tags = reviewTagMapper.selectTagsByReviewId(review.getId());
            review.setTags(tags.stream().map(t -> {
                TagVO tv = new TagVO();
                tv.setId(t.getId());
                tv.setTagName(t.getTagName());
                return tv;
            }).collect(Collectors.toList()));
        }
        return reviews;
    }

    @Override
    @Transactional
    public void publishReview(Long studentId, ReviewRequest request) {
        validateReviewContent(request.getContent());
        ensureNoActiveReviewForCourse(studentId, request.getCourseId(), request.getTeacherId());

        Review review = new Review();
        review.setStudentId(studentId);
        review.setCourseId(request.getCourseId());
        review.setTeacherId(request.getTeacherId());
        review.setOverallScore(calculateOverallScore(request));
        review.setGradingScore(request.getGradingScore());
        review.setTeachingScore(request.getTeachingScore());
        review.setWorkloadScore(request.getWorkloadScore());
        review.setContent(request.getContent());
        review.setStudyTips(request.getStudyTips());
        review.setExamType(request.getExamType());
        review.setLikeCount(0);
        review.setStatus(ReviewStatus.PENDING_AUDIT.name());
        reviewMapper.insert(review);

        saveTags(review.getId(), request.getTags());
    }

    @Override
    @Transactional
    public void editReview(Long studentId, Long reviewId, ReviewRequest request) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new RuntimeException("评价不存在");
        }
        if (!review.getStudentId().equals(studentId)) {
            throw new RuntimeException("无权修改他人评价");
        }

        validateReviewContent(request.getContent());

        review.setOverallScore(calculateOverallScore(request));
        review.setGradingScore(request.getGradingScore());
        review.setTeachingScore(request.getTeachingScore());
        review.setWorkloadScore(request.getWorkloadScore());
        review.setContent(request.getContent());
        review.setStudyTips(request.getStudyTips());
        review.setExamType(request.getExamType());
        review.setStatus(ReviewStatus.PENDING_AUDIT.name());
        reviewMapper.updateById(review);

        reviewTagMapper.deleteByReviewId(reviewId);
        saveTags(reviewId, request.getTags());

        // Once a published review is edited, it should stop contributing to aggregates
        // until it is approved again.
        refreshAggregates(review.getCourseId(), review.getTeacherId());
    }

    @Override
    @Transactional
    public void deleteReview(Long studentId, Long reviewId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new RuntimeException("评价不存在");
        }
        if (!review.getStudentId().equals(studentId)) {
            throw new RuntimeException("无权删除他人评价");
        }
        reviewTagMapper.deleteByReviewId(reviewId);
        reviewMapper.deleteById(reviewId);
        refreshAggregates(review.getCourseId(), review.getTeacherId());
    }

    @Override
    @Transactional
    public VoteResultVO toggleLikeReview(Long studentId, Long reviewId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new RuntimeException("评价不存在");
        }
        if (!ReviewStatus.PUBLISHED.name().equals(review.getStatus()) && !"APPROVED".equals(review.getStatus())) {
            throw new RuntimeException("只能点赞已发布评价");
        }

        LambdaQueryWrapper<ReviewVote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewVote::getReviewId, reviewId)
                .eq(ReviewVote::getStudentId, studentId);
        ReviewVote existingVote = reviewVoteMapper.selectOne(wrapper);

        boolean liked;
        if (existingVote == null) {
            ReviewVote vote = new ReviewVote();
            vote.setReviewId(reviewId);
            vote.setStudentId(studentId);
            reviewVoteMapper.insert(vote);
            reviewMapper.incrementLike(reviewId);
            liked = true;
        } else {
            reviewVoteMapper.deleteById(existingVote.getId());
            reviewMapper.decrementLike(reviewId);
            liked = false;
        }

        Review updatedReview = reviewMapper.selectById(reviewId);
        return new VoteResultVO(updatedReview.getLikeCount(), liked);
    }

    @Override
    public List<ReviewVO> getPendingReviews() {
        List<ReviewVO> reviews = reviewMapper.selectPendingReviews();
        for (ReviewVO review : reviews) {
            List<Tag> tags = reviewTagMapper.selectTagsByReviewId(review.getId());
            review.setTags(tags.stream().map(t -> {
                TagVO tv = new TagVO();
                tv.setId(t.getId());
                tv.setTagName(t.getTagName());
                return tv;
            }).collect(Collectors.toList()));
        }
        return reviews;
    }

    @Override
    public List<Long> getLikedReviewIds(Long studentId, Long courseId) {
        return reviewVoteMapper.selectLikedReviewIds(studentId, courseId);
    }

    @Override
    @Transactional
    public void approveReview(Long adminId, Long reviewId, String reason) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new RuntimeException("评价不存在");
        }
        review.setStatus(ReviewStatus.PUBLISHED.name());
        reviewMapper.updateById(review);
        writeAuditLog(adminId, reviewId, null, "APPROVE_REVIEW", defaultReason(reason, "审核通过"));
        refreshAggregates(review.getCourseId(), review.getTeacherId());
    }

    @Override
    @Transactional
    public void rejectReview(Long adminId, Long reviewId, String reason) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new RuntimeException("评价不存在");
        }
        review.setStatus(ReviewStatus.HIDDEN.name());
        reviewMapper.updateById(review);
        writeAuditLog(adminId, reviewId, null, "HIDE_REVIEW", defaultReason(reason, "审核未通过"));
        refreshAggregates(review.getCourseId(), review.getTeacherId());
    }

    @Override
    @Transactional
    public void adminDeleteReview(Long adminId, Long reviewId, String reason) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new RuntimeException("评价不存在");
        }
        reviewTagMapper.deleteByReviewId(reviewId);
        reviewMapper.deleteById(reviewId);
        writeAuditLog(adminId, reviewId, null, "DELETE_REVIEW", defaultReason(reason, "管理员删除评价"));
        refreshAggregates(review.getCourseId(), review.getTeacherId());
    }

    private int calculateOverallScore(ReviewRequest request) {
        return (int) Math.round(
                (request.getGradingScore() + request.getTeachingScore() + request.getWorkloadScore()) / 3.0);
    }

    private void validateReviewContent(String content) {
        if (sensitiveWordFilter.containsSensitiveWord(content)) {
            throw new RuntimeException("评价内容包含敏感词，请修改后重试");
        }
    }

    private void ensureNoActiveReviewForCourse(Long studentId, Long courseId, Long teacherId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getStudentId, studentId)
                .eq(Review::getCourseId, courseId)
                .eq(Review::getTeacherId, teacherId)
                .in(Review::getStatus,
                        ReviewStatus.PENDING_AUDIT.name(),
                        ReviewStatus.PENDING_MANUAL.name(),
                        ReviewStatus.PUBLISHED.name(),
                        ReviewStatus.PENDING.name(),
                        ReviewStatus.APPROVED.name());
        if (reviewMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("您已经评价过该课程，请勿重复提交");
        }
    }

    private void saveTags(Long reviewId, List<Long> tags) {
        if (tags == null) {
            return;
        }
        for (Long tagId : tags) {
            reviewTagMapper.insertTag(reviewId, tagId);
        }
    }

    private void refreshAggregates(Long courseId, Long teacherId) {
        courseMapper.updateScores(courseId);
        teacherMapper.updateAvgScore(teacherId);
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
}
