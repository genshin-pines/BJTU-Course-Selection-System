package com.bjtu.review.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bjtu.review.dto.CourseSearchRequest;
import com.bjtu.review.entity.Course;
import com.bjtu.review.entity.CourseInstance;
import com.bjtu.review.mapper.CourseInstanceMapper;
import com.bjtu.review.mapper.CourseMapper;
import com.bjtu.review.mapper.ReviewTagMapper;
import com.bjtu.review.mapper.TeacherMapper;
import com.bjtu.review.service.CourseService;
import com.bjtu.review.service.PageResult;
import com.bjtu.review.vo.CourseInstanceVO;
import com.bjtu.review.vo.CourseVO;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    private final TeacherMapper teacherMapper;
    private final ReviewTagMapper reviewTagMapper;
    private final CourseInstanceMapper courseInstanceMapper;

    public CourseServiceImpl(TeacherMapper teacherMapper, ReviewTagMapper reviewTagMapper,
                             CourseInstanceMapper courseInstanceMapper) {
        this.teacherMapper = teacherMapper;
        this.reviewTagMapper = reviewTagMapper;
        this.courseInstanceMapper = courseInstanceMapper;
    }

    @Override
    public CourseVO getCourseDetail(Long id) {
        Course course = baseMapper.selectById(id);
        if (course == null) {
            throw new RuntimeException("Course does not exist");
        }

        CourseVO vo = new CourseVO();
        vo.setId(course.getId());
        vo.setCourseCode(course.getCourseCode());
        vo.setCourseName(course.getCourseName());
        vo.setCredit(course.getCredit());
        vo.setDepartment(course.getDepartment());
        vo.setTeacherId(course.getTeacherId());
        vo.setAvgScore(course.getAvgScore());
        vo.setGradingScore(course.getGradingScore());
        vo.setAvgTeachingScore(course.getAvgTeachingScore());
        vo.setAvgWorkloadScore(course.getAvgWorkloadScore());
        vo.setReviewCount(course.getReviewCount());

        if (course.getTeacherId() != null) {
            var teacher = teacherMapper.selectById(course.getTeacherId());
            if (teacher != null) {
                vo.setTeacherName(teacher.getTeacherName());
            }
        }

        CourseInstance instance = courseInstanceMapper.selectByLegacyCourseId(course.getId());
        if (instance != null) {
            vo.setCourseBaseId(instance.getCourseBaseId());
            vo.setCourseInstanceId(instance.getId());
            vo.setSemester(instance.getSemester());
            vo.setClassName(instance.getClassName());
        }

        return vo;
    }

    @Override
    public CourseVO getCourseInstanceDetail(Long instanceId) {
        CourseVO vo = courseInstanceMapper.selectDetailByInstanceId(instanceId);
        if (vo == null) {
            throw new RuntimeException("Course instance does not exist");
        }
        return vo;
    }

    @Override
    public PageResult<CourseVO> searchCourses(CourseSearchRequest request) {
        int page = request.getPage() == null || request.getPage() < 1 ? 1 : request.getPage();
        int size = request.getSize() == null || request.getSize() < 1 ? 10 : Math.min(request.getSize(), 50);
        int offset = (page - 1) * size;
        List<CourseVO> courses = baseMapper.searchCourses(
                request.getKeyword(),
                request.getDepartment(),
                request.getTeacherName(),
                request.getSemester(),
                request.getMinScore(),
                request.getMaxScore(),
                request.getMinGradingScore(),
                request.getMaxGradingScore(),
                request.getMinTeachingScore(),
                request.getMaxTeachingScore(),
                request.getMinWorkloadScore(),
                request.getMaxWorkloadScore(),
                request.getMinReviewCount(),
                request.getTagIds(),
                request.getSortBy(),
                request.getSortOrder(),
                offset,
                size
        );
        long total = baseMapper.countSearch(
                request.getKeyword(),
                request.getDepartment(),
                request.getTeacherName(),
                request.getSemester(),
                request.getMinScore(),
                request.getMaxScore(),
                request.getMinGradingScore(),
                request.getMaxGradingScore(),
                request.getMinTeachingScore(),
                request.getMaxTeachingScore(),
                request.getMinWorkloadScore(),
                request.getMaxWorkloadScore(),
                request.getMinReviewCount(),
                request.getTagIds()
        );
        return new PageResult<>(courses, total, page, size);
    }

    @Override
    public List<CourseInstanceVO> getCourseInstances(Long id) {
        Course course = baseMapper.selectById(id);
        if (course == null) {
            throw new RuntimeException("Course does not exist");
        }
        CourseInstance instance = courseInstanceMapper.selectByLegacyCourseId(id);
        if (instance == null) {
            return Collections.emptyList();
        }
        return courseInstanceMapper.selectVOByCourseBaseId(instance.getCourseBaseId());
    }
}
