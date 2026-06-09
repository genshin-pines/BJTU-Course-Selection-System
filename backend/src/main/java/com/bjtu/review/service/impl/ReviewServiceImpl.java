package com.bjtu.review.service.impl;

import com.bjtu.review.common.ReviewStatus;
import com.bjtu.review.dto.ReviewRequest;
import com.bjtu.review.entity.Review;
import com.bjtu.review.entity.Tag;
import com.bjtu.review.mapper.CourseMapper;
import com.bjtu.review.mapper.ReviewMapper;
import com.bjtu.review.mapper.ReviewTagMapper;
import com.bjtu.review.mapper.TeacherMapper;
import com.bjtu.review.service.ReviewService;
import com.bjtu.review.utils.SensitiveWordFilter;
import com.bjtu.review.vo.ReviewVO;
import com.bjtu.review.vo.TagVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final ReviewTagMapper reviewTagMapper;
    private final CourseMapper courseMapper;
    private final TeacherMapper teacherMapper;
    private final SensitiveWordFilter sensitiveWordFilter;

    public ReviewServiceImpl(ReviewMapper reviewMapper, ReviewTagMapper reviewTagMapper,
                             CourseMapper courseMapper, TeacherMapper teacherMapper,
                             SensitiveWordFilter sensitiveWordFilter) {
        this.reviewMapper = reviewMapper;
        this.reviewTagMapper = reviewTagMapper;
        this.courseMapper = courseMapper;
        this.teacherMapper = teacherMapper;
        this.sensitiveWordFilter = sensitiveWordFilter;
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
        // 敏感词过滤
        if (sensitiveWordFilter.containsSensitiveWord(request.getContent())) {
            throw new RuntimeException("评价内容包含敏感词，请修改后重试");
        }

        Review review = new Review();
        review.setStudentId(studentId);
        review.setCourseId(request.getCourseId());
        review.setTeacherId(request.getTeacherId());
        // overallScore 自动计算为三项平均分（四舍五入取整）
        int autoOverall = (int) Math.round(
            (request.getGradingScore() + request.getTeachingScore() + request.getWorkloadScore()) / 3.0);
        review.setOverallScore(autoOverall);
        review.setGradingScore(request.getGradingScore());
        review.setTeachingScore(request.getTeachingScore());
        review.setWorkloadScore(request.getWorkloadScore());
        review.setContent(request.getContent());
        review.setStudyTips(request.getStudyTips());
        review.setExamType(request.getExamType());
        review.setLikeCount(0);
        review.setStatus(ReviewStatus.APPROVED.name());
        reviewMapper.insert(review);

        // 保存标签关联
        if (request.getTags() != null) {
            for (Long tagId : request.getTags()) {
                reviewTagMapper.insertTag(review.getId(), tagId);
            }
        }

        // 即时更新课程和教师评分
        courseMapper.updateScores(request.getCourseId());
        teacherMapper.updateAvgScore(request.getTeacherId());
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
        if (sensitiveWordFilter.containsSensitiveWord(request.getContent())) {
            throw new RuntimeException("评价内容包含敏感词，请修改后重试");
        }

        // overallScore 自动计算为三项平均分（四舍五入取整）
        int autoOverall = (int) Math.round(
            (request.getGradingScore() + request.getTeachingScore() + request.getWorkloadScore()) / 3.0);
        review.setOverallScore(autoOverall);
        review.setGradingScore(request.getGradingScore());
        review.setTeachingScore(request.getTeachingScore());
        review.setWorkloadScore(request.getWorkloadScore());
        review.setContent(request.getContent());
        review.setStudyTips(request.getStudyTips());
        review.setExamType(request.getExamType());
        review.setStatus(ReviewStatus.APPROVED.name());
        reviewMapper.updateById(review);

        // 更新标签
        reviewTagMapper.deleteByReviewId(reviewId);
        if (request.getTags() != null) {
            for (Long tagId : request.getTags()) {
                reviewTagMapper.insertTag(reviewId, tagId);
            }
        }
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
        courseMapper.updateScores(review.getCourseId());
        teacherMapper.updateAvgScore(review.getTeacherId());
    }

    @Override
    public void likeReview(Long reviewId) {
        reviewMapper.incrementLike(reviewId);
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
    @Transactional
    public void approveReview(Long reviewId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new RuntimeException("评价不存在");
        }
        review.setStatus(ReviewStatus.APPROVED.name());
        reviewMapper.updateById(review);
        // 更新课程和教师评分
        courseMapper.updateScores(review.getCourseId());
        teacherMapper.updateAvgScore(review.getTeacherId());
    }

    @Override
    @Transactional
    public void rejectReview(Long reviewId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new RuntimeException("评价不存在");
        }
        review.setStatus(ReviewStatus.REJECTED.name());
        reviewMapper.updateById(review);
        // 更新课程和教师评分 (可能之前是 APPROVED 状态)
        courseMapper.updateScores(review.getCourseId());
        teacherMapper.updateAvgScore(review.getTeacherId());
    }

    @Override
    @Transactional
    public void adminDeleteReview(Long reviewId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new RuntimeException("评价不存在");
        }
        reviewTagMapper.deleteByReviewId(reviewId);
        reviewMapper.deleteById(reviewId);
        courseMapper.updateScores(review.getCourseId());
        teacherMapper.updateAvgScore(review.getTeacherId());
    }
}
