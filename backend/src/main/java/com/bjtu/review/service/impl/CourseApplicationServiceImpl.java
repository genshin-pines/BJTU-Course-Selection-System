package com.bjtu.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bjtu.review.dto.CourseApplicationRequest;
import com.bjtu.review.dto.ReviewRequest;
import com.bjtu.review.entity.*;
import com.bjtu.review.mapper.*;
import com.bjtu.review.service.CourseApplicationService;
import com.bjtu.review.service.ReviewService;
import com.bjtu.review.vo.CourseApplicationVO;
import com.bjtu.review.vo.ReviewPublishResult;
import com.bjtu.review.vo.TagVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseApplicationServiceImpl implements CourseApplicationService {

    private final CourseApplicationMapper courseApplicationMapper;
    private final CourseBaseMapper courseBaseMapper;
    private final TeacherMapper teacherMapper;
    private final CourseInstanceMapper courseInstanceMapper;
    private final StudentMapper studentMapper;
    private final TagMapper tagMapper;
    private final ReviewService reviewService;

    public CourseApplicationServiceImpl(CourseApplicationMapper courseApplicationMapper,
                                        CourseBaseMapper courseBaseMapper,
                                        TeacherMapper teacherMapper,
                                        CourseInstanceMapper courseInstanceMapper,
                                        StudentMapper studentMapper,
                                        TagMapper tagMapper,
                                        ReviewService reviewService) {
        this.courseApplicationMapper = courseApplicationMapper;
        this.courseBaseMapper = courseBaseMapper;
        this.teacherMapper = teacherMapper;
        this.courseInstanceMapper = courseInstanceMapper;
        this.studentMapper = studentMapper;
        this.tagMapper = tagMapper;
        this.reviewService = reviewService;
    }

    @Override
    @Transactional
    public Long submitApplication(Long studentId, CourseApplicationRequest request) {
        // 检查课程代码是否已存在
        CourseBase existingCourse = courseBaseMapper.selectOne(
                new LambdaQueryWrapper<CourseBase>()
                        .eq(CourseBase::getCourseCode, request.getCourseCode().trim())
                        .last("LIMIT 1"));
        if (existingCourse != null) {
            throw new RuntimeException("课程代码已存在，请直接搜索该课程进行评价");
        }

        CourseApplication app = new CourseApplication();
        app.setStudentId(studentId);
        app.setCourseCode(request.getCourseCode().trim());
        app.setCourseName(request.getCourseName().trim());
        app.setCredit(request.getCredit() == null ? 0 : request.getCredit());
        app.setDepartment(trimOrNull(request.getDepartment()));
        app.setTeacherName(request.getTeacherName().trim());
        app.setTeacherDepartment(trimOrNull(request.getTeacherDepartment()));
        app.setGradingScore(request.getGradingScore());
        app.setTeachingScore(request.getTeachingScore());
        app.setWorkloadScore(request.getWorkloadScore());
        app.setContent(request.getContent().trim());
        app.setStudyTips(trimOrNull(request.getStudyTips()));
        app.setExamType(trimOrNull(request.getExamType()));
        app.setKeyChapters(trimOrNull(request.getKeyChapters()));
        app.setCheatSheetAllowed(request.getCheatSheetAllowed());
        app.setTagIds(request.getTagIds() == null || request.getTagIds().isEmpty()
                ? null
                : request.getTagIds().stream().map(String::valueOf).collect(Collectors.joining(",")));
        app.setStatus("PENDING");
        courseApplicationMapper.insert(app);
        return app.getId();
    }

    @Override
    public List<CourseApplicationVO> getMyApplications(Long studentId) {
        List<CourseApplication> list = courseApplicationMapper.selectList(
                new LambdaQueryWrapper<CourseApplication>()
                        .eq(CourseApplication::getStudentId, studentId)
                        .orderByDesc(CourseApplication::getCreateTime));
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<CourseApplicationVO> getPendingApplications() {
        List<CourseApplication> list = courseApplicationMapper.selectList(
                new LambdaQueryWrapper<CourseApplication>()
                        .eq(CourseApplication::getStatus, "PENDING")
                        .orderByAsc(CourseApplication::getCreateTime));
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void approve(Long adminId, Long applicationId, String reason) {
        CourseApplication app = requireApplication(applicationId);
        if (!"PENDING".equals(app.getStatus())) {
            throw new RuntimeException("该申请已处理");
        }

        // 1. 创建或复用课程
        CourseBase course = courseBaseMapper.selectOne(
                new LambdaQueryWrapper<CourseBase>()
                        .eq(CourseBase::getCourseCode, app.getCourseCode())
                        .last("LIMIT 1"));
        if (course == null) {
            course = new CourseBase();
            course.setCourseCode(app.getCourseCode());
            course.setCourseName(app.getCourseName());
            course.setCredit(app.getCredit());
            course.setDepartment(app.getDepartment());
            courseBaseMapper.insert(course);
        }

        // 2. 创建或复用教师
        Teacher teacher = teacherMapper.selectOne(
                new LambdaQueryWrapper<Teacher>()
                        .eq(Teacher::getTeacherName, app.getTeacherName())
                        .last("LIMIT 1"));
        if (teacher == null) {
            teacher = new Teacher();
            teacher.setTeacherName(app.getTeacherName());
            teacher.setDepartment(app.getTeacherDepartment() != null ? app.getTeacherDepartment() : app.getDepartment());
            teacher.setAvgScore(0.0);
            teacher.setAvgTeachingScore(0.0);
            teacher.setAvgWorkloadScore(0.0);
            teacherMapper.insert(teacher);
        }

        // 3. 创建或复用开课实例
        CourseInstance instance = courseInstanceMapper.selectOne(
                new LambdaQueryWrapper<CourseInstance>()
                        .eq(CourseInstance::getCourseBaseId, course.getId())
                        .eq(CourseInstance::getTeacherId, teacher.getId())
                        .last("LIMIT 1"));
        if (instance == null) {
            instance = new CourseInstance();
            instance.setCourseBaseId(course.getId());
            instance.setTeacherId(teacher.getId());
            instance.setAvgScore(0.0);
            instance.setGradingScore(0.0);
            instance.setAvgTeachingScore(0.0);
            instance.setAvgWorkloadScore(0.0);
            instance.setReviewCount(0);
            courseInstanceMapper.insert(instance);
        }

        // 4. 以申请人身份发布评价
        ReviewRequest reviewRequest = new ReviewRequest();
        reviewRequest.setCourseInstanceId(instance.getId());
        reviewRequest.setCourseId(course.getId());
        reviewRequest.setTeacherId(teacher.getId());
        reviewRequest.setGradingScore(app.getGradingScore());
        reviewRequest.setTeachingScore(app.getTeachingScore());
        reviewRequest.setWorkloadScore(app.getWorkloadScore());
        reviewRequest.setContent(app.getContent());
        reviewRequest.setStudyTips(app.getStudyTips());
        reviewRequest.setExamType(app.getExamType());
        reviewRequest.setKeyChapters(app.getKeyChapters());
        reviewRequest.setCheatSheetAllowed(app.getCheatSheetAllowed());
        reviewRequest.setTags(parseTagIds(app.getTagIds()));

        ReviewPublishResult publishResult = reviewService.publishReview(app.getStudentId(), reviewRequest);

        // 5. 更新申请状态
        app.setStatus("APPROVED");
        app.setReviewReason(reason != null && !reason.isBlank() ? reason.trim() : "审核通过，已创建课程并发布评价");
        app.setReviewerAdminId(adminId);
        app.setCreatedInstanceId(instance.getId());
        app.setCreatedReviewId(publishResult.getReviewId());
        app.setReviewTime(LocalDateTime.now());
        courseApplicationMapper.updateById(app);
    }

    @Override
    @Transactional
    public void reject(Long adminId, Long applicationId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new RuntimeException("拒绝申请必须填写原因");
        }
        CourseApplication app = requireApplication(applicationId);
        if (!"PENDING".equals(app.getStatus())) {
            throw new RuntimeException("该申请已处理");
        }
        app.setStatus("REJECTED");
        app.setReviewReason(reason.trim());
        app.setReviewerAdminId(adminId);
        app.setReviewTime(LocalDateTime.now());
        courseApplicationMapper.updateById(app);
    }

    // ===== 内部方法 =====

    private CourseApplication requireApplication(Long id) {
        CourseApplication app = courseApplicationMapper.selectById(id);
        if (app == null) {
            throw new RuntimeException("课程申请不存在");
        }
        return app;
    }

    private CourseApplicationVO toVO(CourseApplication app) {
        CourseApplicationVO vo = new CourseApplicationVO();
        vo.setId(app.getId());
        // 匿名ID
        Student student = studentMapper.selectById(app.getStudentId());
        vo.setStudentAnonymousId(student != null ? student.getAnonymousId() : "匿名用户");
        vo.setCourseCode(app.getCourseCode());
        vo.setCourseName(app.getCourseName());
        vo.setCredit(app.getCredit());
        vo.setDepartment(app.getDepartment());
        vo.setTeacherName(app.getTeacherName());
        vo.setGradingScore(app.getGradingScore());
        vo.setTeachingScore(app.getTeachingScore());
        vo.setWorkloadScore(app.getWorkloadScore());
        vo.setContent(app.getContent());
        vo.setStudyTips(app.getStudyTips());
        vo.setExamType(app.getExamType());
        vo.setKeyChapters(app.getKeyChapters());
        vo.setCheatSheetAllowed(app.getCheatSheetAllowed());
        vo.setTags(parseTags(app.getTagIds()));
        vo.setStatus(app.getStatus());
        vo.setReviewReason(app.getReviewReason());
        vo.setCreatedInstanceId(app.getCreatedInstanceId());
        vo.setCreateTime(app.getCreateTime());
        vo.setReviewTime(app.getReviewTime());
        return vo;
    }

    private List<TagVO> parseTags(String tagIdsStr) {
        if (tagIdsStr == null || tagIdsStr.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(tagIdsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .map(id -> {
                    Tag tag = tagMapper.selectById(id);
                    if (tag == null) return null;
                    TagVO tv = new TagVO();
                    tv.setId(tag.getId());
                    tv.setTagName(tag.getTagName());
                    return tv;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<Long> parseTagIds(String tagIdsStr) {
        if (tagIdsStr == null || tagIdsStr.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(tagIdsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    private String trimOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
