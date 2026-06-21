package com.bjtu.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bjtu.review.common.ReviewStatus;
import com.bjtu.review.dto.ReviewRequest;
import com.bjtu.review.entity.AuditLog;
import com.bjtu.review.entity.CourseInstance;
import com.bjtu.review.entity.Review;
import com.bjtu.review.entity.ReviewVote;
import com.bjtu.review.entity.Tag;
import com.bjtu.review.entity.VoterRecord;
import com.bjtu.review.mapper.AuditLogMapper;
import com.bjtu.review.mapper.CourseInstanceMapper;
import com.bjtu.review.mapper.ReviewExamExpMapper;
import com.bjtu.review.mapper.ReviewMapper;
import com.bjtu.review.mapper.ReviewTagMapper;
import com.bjtu.review.mapper.ReviewVoteMapper;
import com.bjtu.review.mapper.TeacherMapper;
import com.bjtu.review.service.AnonymityService;
import com.bjtu.review.service.ReviewService;
import com.bjtu.review.service.PageResult;
import com.bjtu.review.utils.SensitiveWordFilter;
import com.bjtu.review.vo.ReviewPublishResult;
import com.bjtu.review.vo.ReviewVO;
import com.bjtu.review.vo.TagVO;
import com.bjtu.review.vo.VoteResultVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final String AUTO_HIDE_REASON = "违禁词自动拦截";

    private final ReviewMapper reviewMapper;
    private final ReviewTagMapper reviewTagMapper;
    private final ReviewVoteMapper reviewVoteMapper;
    private final TeacherMapper teacherMapper;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final AuditLogMapper auditLogMapper;
    private final AnonymityService anonymityService;
    private final CourseInstanceMapper courseInstanceMapper;
    private final ReviewExamExpMapper reviewExamExpMapper;

    public ReviewServiceImpl(ReviewMapper reviewMapper, ReviewTagMapper reviewTagMapper,
                             ReviewVoteMapper reviewVoteMapper,
                             TeacherMapper teacherMapper,
                             SensitiveWordFilter sensitiveWordFilter,
                             AuditLogMapper auditLogMapper,
                             AnonymityService anonymityService,
                             CourseInstanceMapper courseInstanceMapper,
                             ReviewExamExpMapper reviewExamExpMapper) {
        this.reviewMapper = reviewMapper;
        this.reviewTagMapper = reviewTagMapper;
        this.reviewVoteMapper = reviewVoteMapper;
        this.teacherMapper = teacherMapper;
        this.sensitiveWordFilter = sensitiveWordFilter;
        this.auditLogMapper = auditLogMapper;
        this.anonymityService = anonymityService;
        this.courseInstanceMapper = courseInstanceMapper;
        this.reviewExamExpMapper = reviewExamExpMapper;
    }

    @Override
    public List<ReviewVO> getReviewsByCourseId(Long courseId) {
        return getReviewsByCourseId(courseId, null, null);
    }

    @Override
    public List<ReviewVO> getReviewsByCourseId(Long courseId, Long courseInstanceId) {
        return getReviewsByCourseId(courseId, courseInstanceId, null);
    }

    @Override
    public List<ReviewVO> getReviewsByCourseId(Long courseId, Long courseInstanceId, String sortBy) {
        return getReviewsByCourseId(courseId, courseInstanceId, sortBy, null);
    }

    @Override
    public List<ReviewVO> getReviewsByCourseId(Long courseId, Long courseInstanceId, String sortBy, List<Long> tagIds) {
        return getReviewsByCourseId(courseId, courseInstanceId, sortBy, tagIds, null);
    }

    @Override
    public List<ReviewVO> getReviewsByCourseId(Long courseId, Long courseInstanceId, String sortBy,
                                               List<Long> tagIds, Long studentId) {
        List<ReviewVO> reviews = reviewMapper.selectByCourseId(
                courseId,
                normalizeInstanceFilter(courseInstanceId),
                normalizeReviewSort(sortBy),
                normalizeTagIds(tagIds));
        enrichReviews(reviews, studentId);
        return reviews;
    }

    @Override
    public List<ReviewVO> getReviewsByCourseInstanceId(Long courseInstanceId) {
        return getReviewsByCourseInstanceId(courseInstanceId, null);
    }

    @Override
    public List<ReviewVO> getReviewsByCourseInstanceId(Long courseInstanceId, String sortBy) {
        return getReviewsByCourseInstanceId(courseInstanceId, sortBy, null);
    }

    @Override
    public List<ReviewVO> getReviewsByCourseInstanceId(Long courseInstanceId, String sortBy, List<Long> tagIds) {
        return getReviewsByCourseInstanceId(courseInstanceId, sortBy, tagIds, null);
    }

    @Override
    public List<ReviewVO> getReviewsByCourseInstanceId(Long courseInstanceId, String sortBy,
                                                      List<Long> tagIds, Long studentId) {
        List<ReviewVO> reviews = reviewMapper.selectByCourseInstanceId(
                courseInstanceId,
                normalizeReviewSort(sortBy),
                normalizeTagIds(tagIds));
        enrichReviews(reviews, studentId);
        return reviews;
    }

    @Override
    @Transactional
    public ReviewPublishResult publishReview(Long studentId, ReviewRequest request) {
        CourseInstance instance = resolveCourseInstance(request);
        Long courseInstanceId = instance.getId();
        Long courseBaseId = instance.getCourseBaseId();
        Long teacherId = instance.getTeacherId();

        VoterRecord voterRecord = anonymityService.getOrCreateCourseReviewRecord(
                studentId, courseBaseId, teacherId, courseInstanceId);
        removePreviousHiddenReviews(voterRecord.getId(), courseBaseId, teacherId, courseInstanceId);
        ensureNoActiveReviewForCourse(voterRecord.getId(), courseBaseId, teacherId, courseInstanceId);


        boolean sensitive = containsSensitiveContent(request);
        Review review = new Review();
        review.setVoterRecordId(voterRecord.getId());
        review.setAnonymousUserKey(voterRecord.getAnonymousKey());
        review.setCourseInstanceId(courseInstanceId);
        review.setCourseId(courseBaseId);
        review.setTeacherId(teacherId);
        review.setOverallScore(calculateOverallScore(request));
        review.setGradingScore(request.getGradingScore());
        review.setTeachingScore(request.getTeachingScore());
        review.setWorkloadScore(request.getWorkloadScore());
        review.setContent(request.getContent());
        review.setStudyTips(request.getStudyTips());
        review.setExamType(request.getExamType());
        review.setLikeCount(0);
        review.setDownvoteCount(0);
        if (sensitive) {
            review.setStatus(ReviewStatus.HIDDEN.name());
            review.setHideReason(AUTO_HIDE_REASON);
        } else {
            review.setStatus(ReviewStatus.PUBLISHED.name());
            review.setHideReason(null);
        }
        reviewMapper.insert(review);

        saveExamExperience(review.getId(), request);
        saveTags(review.getId(), request.getTags());
        if (!sensitive) {
            refreshAggregates(review);
            return ReviewPublishResult.published(review.getId());
        }
        return ReviewPublishResult.autoHidden(review.getId());
    }

    @Override
    @Transactional
    public ReviewPublishResult editReview(Long studentId, Long reviewId, ReviewRequest request) {
        Review review = requireReview(reviewId);
        if (!canStudentOperateReview(studentId, review)) {
            throw new RuntimeException("无权修改他人评价");
        }
        if (!isEditableStatus(review.getStatus())) {
            throw new RuntimeException("当前评价不可修改");
        }

        boolean sensitive = containsSensitiveContent(request);
        review.setOverallScore(calculateOverallScore(request));
        review.setGradingScore(request.getGradingScore());
        review.setTeachingScore(request.getTeachingScore());
        review.setWorkloadScore(request.getWorkloadScore());
        review.setContent(request.getContent());
        review.setStudyTips(request.getStudyTips());
        review.setExamType(request.getExamType());
        if (sensitive) {
            review.setStatus(ReviewStatus.HIDDEN.name());
            review.setHideReason(AUTO_HIDE_REASON);
        } else {
            review.setStatus(ReviewStatus.PUBLISHED.name());
            review.setHideReason(null);
        }
        reviewMapper.updateById(review);

        saveExamExperience(reviewId, request);
        reviewTagMapper.deleteByReviewId(reviewId);
        saveTags(reviewId, request.getTags());
        refreshAggregates(review);
        return sensitive ? ReviewPublishResult.autoHidden(reviewId) : ReviewPublishResult.republishedAfterEdit(reviewId);
    }

    @Override
    public ReviewVO getStudentReview(Long studentId, Long reviewId) {
        Review review = requireReview(reviewId);
        if (!canStudentOperateReview(studentId, review)) {
            throw new RuntimeException("无权查看他人评价");
        }
        if (!isEditableStatus(review.getStatus())) {
            throw new RuntimeException("当前评价不可修改");
        }
        ReviewVO reviewVO = reviewMapper.selectReviewVoById(reviewId);
        if (reviewVO == null) {
            throw new RuntimeException("评价不存在");
        }
        attachTags(reviewVO);
        reviewVO.setIsOwner(true);
        return reviewVO;
    }

    @Override
    public ReviewVO getMyReviewForCourse(Long studentId, Long courseId, Long courseInstanceId, Long teacherId) {
        if (courseId == null || teacherId == null) {
            return null;
        }
        ReviewVO reviewVO = reviewMapper.selectMyReviewForStudent(
                studentId,
                normalizeLegacyCourseId(courseId),
                teacherId,
                normalizeInstanceFilter(courseInstanceId));
        if (reviewVO == null) {
            return null;
        }
        attachTags(reviewVO);
        reviewVO.setIsOwner(true);
        return reviewVO;
    }

    @Override
    @Transactional
    public void deleteReview(Long studentId, Long reviewId) {
        Review review = requireReview(reviewId);
        if (!canStudentOperateReview(studentId, review)) {
            throw new RuntimeException("无权删除他人评价");
        }
        reviewExamExpMapper.deleteByReviewId(reviewId);
        reviewTagMapper.deleteByReviewId(reviewId);
        reviewMapper.deleteById(reviewId);
        refreshAggregates(review);
    }

    @Override
    @Transactional
    public VoteResultVO toggleLikeReview(Long studentId, Long reviewId) {
        return toggleVoteReview(studentId, reviewId, "UPVOTE");
    }

    @Override
    @Transactional
    public VoteResultVO toggleDownvoteReview(Long studentId, Long reviewId) {
        return toggleVoteReview(studentId, reviewId, "DOWNVOTE");
    }

    private VoteResultVO toggleVoteReview(Long studentId, Long reviewId, String targetVoteType) {
        Review review = requireReview(reviewId);
        if (!ReviewStatus.PUBLISHED.name().equals(review.getStatus()) && !"APPROVED".equals(review.getStatus())) {
            throw new RuntimeException("只能投票已发布评价");
        }
        if (review.getVoterRecordId() != null && anonymityService.isOwner(studentId, review.getVoterRecordId())) {
            throw new RuntimeException("不能给自己的评价投票");
        }

        VoterRecord voterRecord = anonymityService.getOrCreateCourseReviewRecord(
                studentId, review.getCourseId(), review.getTeacherId(), review.getCourseInstanceId());
        LambdaQueryWrapper<ReviewVote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewVote::getReviewId, reviewId)
                .eq(ReviewVote::getVoterRecordId, voterRecord.getId());
        ReviewVote existingVote = reviewVoteMapper.selectOne(wrapper);

        if (existingVote == null) {
            ReviewVote vote = new ReviewVote();
            vote.setReviewId(reviewId);
            vote.setVoterRecordId(voterRecord.getId());
            vote.setVoteType(targetVoteType);
            reviewVoteMapper.insert(vote);
            incrementVoteCount(reviewId, targetVoteType);
        } else if (targetVoteType.equals(existingVote.getVoteType())) {
            reviewVoteMapper.deleteById(existingVote.getId());
            decrementVoteCount(reviewId, targetVoteType);
        } else {
            String previousVoteType = existingVote.getVoteType();
            existingVote.setVoteType(targetVoteType);
            reviewVoteMapper.updateById(existingVote);
            decrementVoteCount(reviewId, previousVoteType);
            incrementVoteCount(reviewId, targetVoteType);
        }

        Review updatedReview = reviewMapper.selectById(reviewId);
        Integer likeCount = updatedReview.getLikeCount() == null ? 0 : updatedReview.getLikeCount();
        Integer downvoteCount = updatedReview.getDownvoteCount() == null ? 0 : updatedReview.getDownvoteCount();
        ReviewVote finalVote = reviewVoteMapper.selectOne(wrapper);
        String voteType = finalVote == null ? null : finalVote.getVoteType();
        return new VoteResultVO(
                likeCount,
                downvoteCount,
                "UPVOTE".equals(voteType),
                "DOWNVOTE".equals(voteType),
                voteType);
    }

    private void incrementVoteCount(Long reviewId, String voteType) {
        if ("DOWNVOTE".equals(voteType)) {
            reviewMapper.incrementDownvote(reviewId);
            return;
        }
        reviewMapper.incrementLike(reviewId);
    }

    private void decrementVoteCount(Long reviewId, String voteType) {
        if ("DOWNVOTE".equals(voteType)) {
            reviewMapper.decrementDownvote(reviewId);
            return;
        }
        if ("UPVOTE".equals(voteType)) {
            reviewMapper.decrementLike(reviewId);
        }
    }

    private VoteResultVO unusedToggleLikeReview(Long studentId, Long reviewId) {
        Review review = requireReview(reviewId);
        if (!ReviewStatus.PUBLISHED.name().equals(review.getStatus()) && !"APPROVED".equals(review.getStatus())) {
            throw new RuntimeException("只能点赞已发布评价");
        }

        VoterRecord voterRecord = anonymityService.getOrCreateCourseReviewRecord(
                studentId, review.getCourseId(), review.getTeacherId(), review.getCourseInstanceId());
        LambdaQueryWrapper<ReviewVote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewVote::getReviewId, reviewId)
                .eq(ReviewVote::getVoterRecordId, voterRecord.getId());
        ReviewVote existingVote = reviewVoteMapper.selectOne(wrapper);

        boolean liked;
        if (existingVote == null) {
            ReviewVote vote = new ReviewVote();
            vote.setReviewId(reviewId);
            vote.setVoterRecordId(voterRecord.getId());
            vote.setVoteType("UPVOTE");
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
            attachTags(review);
        }
        return reviews;
    }

    @Override
    public List<Long> getLikedReviewIds(Long studentId, Long courseId) {
        return getLikedReviewIds(studentId, courseId, null);
    }

    @Override
    public List<Long> getLikedReviewIds(Long studentId, Long courseId, Long courseInstanceId) {
        return reviewVoteMapper.selectVotedReviewIds(studentId, courseId, normalizeInstanceFilter(courseInstanceId), "UPVOTE");
    }

    @Override
    public List<Long> getLikedReviewIdsByInstance(Long studentId, Long courseInstanceId) {
        return reviewVoteMapper.selectVotedReviewIdsByInstance(studentId, courseInstanceId, "UPVOTE");
    }

    @Override
    public List<Long> getDownvotedReviewIds(Long studentId, Long courseId, Long courseInstanceId) {
        return reviewVoteMapper.selectVotedReviewIds(studentId, courseId, normalizeInstanceFilter(courseInstanceId), "DOWNVOTE");
    }

    @Override
    public List<Long> getDownvotedReviewIdsByInstance(Long studentId, Long courseInstanceId) {
        return reviewVoteMapper.selectVotedReviewIdsByInstance(studentId, courseInstanceId, "DOWNVOTE");
    }

    @Override
    public PageResult<ReviewVO> getAdminReviews(String role,
                                                String scopedDepartment,
                                                String status,
                                                String courseName,
                                                String teacherName,
                                                String department,
                                                LocalDateTime startTime,
                                                LocalDateTime endTime,
                                                int page,
                                                int pageSize) {
        int currentPage = Math.max(page, 1);
        int size = Math.min(Math.max(pageSize, 1), 100);
        int offset = (currentPage - 1) * size;
        long total = reviewMapper.countAdminReviews(
                role, scopedDepartment, status, courseName, teacherName, department, startTime, endTime);
        List<ReviewVO> reviews = reviewMapper.selectAdminReviews(
                role, scopedDepartment, status, courseName, teacherName, department, startTime, endTime, offset, size);
        for (ReviewVO review : reviews) {
            attachTags(review);
        }
        return new PageResult<>(reviews, total, currentPage, size);
    }

    @Override
    @Transactional
    public void approveReview(Long adminId, Long reviewId, String reason) {
        Review review = requireReview(reviewId);
        review.setStatus(ReviewStatus.APPROVED.name());
        review.setHideReason(null);
        reviewMapper.updateById(review);
        writeAuditLog(adminId, reviewId, null, "APPROVE_REVIEW", defaultReason(reason, "审核通过"));
        refreshAggregates(review);
    }

    @Override
    @Transactional
    public void rejectReview(Long adminId, Long reviewId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new RuntimeException("拒绝评价必须填写原因");
        }
        Review review = requireReview(reviewId);
        review.setStatus(ReviewStatus.HIDDEN.name());
        review.setHideReason(reason.trim());
        reviewMapper.updateById(review);
        writeAuditLog(adminId, reviewId, null, "REJECT_REVIEW", reason.trim());
        refreshAggregates(review);
    }

    @Override
    @Transactional
    public void adminDeleteReview(Long adminId, Long reviewId, String reason) {
        Review review = requireReview(reviewId);
        reviewExamExpMapper.deleteByReviewId(reviewId);
        reviewTagMapper.deleteByReviewId(reviewId);
        reviewMapper.deleteById(reviewId);
        writeAuditLog(adminId, reviewId, null, "DELETE_REVIEW", defaultReason(reason, "管理员删除评价"));
        refreshAggregates(review);
    }

    private void attachTags(ReviewVO review) {
        List<Tag> tags = reviewTagMapper.selectTagsByReviewId(review.getId());
        review.setTags(tags.stream().map(t -> {
            TagVO tv = new TagVO();
            tv.setId(t.getId());
            tv.setTagName(t.getTagName());
            return tv;
        }).collect(Collectors.toList()));
    }

    private void enrichReviews(List<ReviewVO> reviews, Long studentId) {
        for (ReviewVO review : reviews) {
            attachTags(review);
            if (studentId != null && review.getVoterRecordId() != null) {
                review.setIsOwner(anonymityService.isOwner(studentId, review.getVoterRecordId()));
            } else {
                review.setIsOwner(false);
            }
        }
    }

    private boolean isEditableStatus(String status) {
        return ReviewStatus.PUBLISHED.name().equals(status)
                || ReviewStatus.APPROVED.name().equals(status)
                || ReviewStatus.HIDDEN.name().equals(status);
    }

    private Review requireReview(Long reviewId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new RuntimeException("评价不存在");
        }
        return review;
    }

    private int calculateOverallScore(ReviewRequest request) {
        return (int) Math.round(
                (request.getGradingScore() + request.getTeachingScore() + request.getWorkloadScore()) / 3.0);
    }

    private boolean containsSensitiveContent(ReviewRequest request) {
        return sensitiveWordFilter.containsSensitiveWord(
                request.getContent(),
                request.getStudyTips(),
                request.getExamType(),
                request.getKeyChapters());
    }

    private void removePreviousHiddenReviews(Long voterRecordId, Long courseId, Long teacherId, Long courseInstanceId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getCourseId, normalizeLegacyCourseId(courseId))
                .eq(Review::getTeacherId, teacherId)
                .eq(Review::getVoterRecordId, voterRecordId)
                .eq(Review::getStatus, ReviewStatus.HIDDEN.name());
        Long instanceFilter = normalizeInstanceFilter(courseInstanceId);
        if (instanceFilter != null) {
            wrapper.eq(Review::getCourseInstanceId, instanceFilter);
        }
        List<Review> hiddenReviews = reviewMapper.selectList(wrapper);
        for (Review hidden : hiddenReviews) {
            reviewExamExpMapper.deleteByReviewId(hidden.getId());
            reviewTagMapper.deleteByReviewId(hidden.getId());
            reviewMapper.deleteById(hidden.getId());
        }
    }

    private void ensureNoActiveReviewForCourse(Long voterRecordId, Long courseBaseId, Long teacherId, Long courseInstanceId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getCourseId, courseBaseId)
                .eq(Review::getTeacherId, teacherId)
                .eq(Review::getCourseInstanceId, courseInstanceId)
                .eq(Review::getVoterRecordId, voterRecordId)
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

    private boolean canStudentOperateReview(Long studentId, Review review) {
        if (review.getVoterRecordId() != null) {
            return anonymityService.isOwner(studentId, review.getVoterRecordId());
        }
        return false;
    }

    private CourseInstance resolveCourseInstance(ReviewRequest request) {
        if (request.getCourseInstanceId() == null || request.getCourseInstanceId() <= 0) {
            throw new RuntimeException("开课实例不能为空");
        }
        CourseInstance instance = courseInstanceMapper.selectById(request.getCourseInstanceId());
        if (instance == null) {
            throw new RuntimeException("开课实例不存在");
        }
        return instance;
    }

    private Long normalizeInstanceFilter(Long courseInstanceId) {
        return courseInstanceId == null || courseInstanceId <= 0 ? null : courseInstanceId;
    }

    private String normalizeReviewSort(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "quality";
        }
        return switch (sortBy) {
            case "quality", "latest", "highScore", "useful", "controversial" -> sortBy;
            default -> "quality";
        };
    }

    private List<Long> normalizeTagIds(List<Long> tagIds) {
        if (tagIds == null) {
            return List.of();
        }
        return tagIds.stream()
                .filter(tagId -> tagId != null && tagId > 0)
                .distinct()
                .toList();
    }

    private Long normalizeLegacyCourseId(Long courseId) {
        return courseId == null || courseId <= 0 ? 0L : courseId;
    }

    private void saveTags(Long reviewId, List<Long> tags) {
        if (tags == null) {
            return;
        }
        for (Long tagId : tags) {
            reviewTagMapper.insertTag(reviewId, tagId);
        }
    }

    private void saveExamExperience(Long reviewId, ReviewRequest request) {
        reviewExamExpMapper.upsertBasicExperience(
                reviewId,
                request.getExamType(),
                request.getStudyTips(),
                request.getKeyChapters(),
                request.getCheatSheetAllowed());
    }

    private void refreshAggregates(Review review) {
        if (review.getCourseInstanceId() != null && review.getCourseInstanceId() > 0) {
            courseInstanceMapper.updateScores(review.getCourseInstanceId());
        }
        teacherMapper.updateAvgScore(review.getTeacherId());
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
