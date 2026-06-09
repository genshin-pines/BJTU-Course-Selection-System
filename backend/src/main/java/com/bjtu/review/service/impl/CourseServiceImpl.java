package com.bjtu.review.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bjtu.review.dto.CourseSearchRequest;
import com.bjtu.review.entity.Course;
import com.bjtu.review.entity.Tag;
import com.bjtu.review.mapper.CourseMapper;
import com.bjtu.review.mapper.ReviewTagMapper;
import com.bjtu.review.mapper.TeacherMapper;
import com.bjtu.review.service.CourseService;
import com.bjtu.review.service.PageResult;
import com.bjtu.review.vo.CourseVO;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    private final TeacherMapper teacherMapper;
    private final ReviewTagMapper reviewTagMapper;

    public CourseServiceImpl(TeacherMapper teacherMapper, ReviewTagMapper reviewTagMapper) {
        this.teacherMapper = teacherMapper;
        this.reviewTagMapper = reviewTagMapper;
    }

    @Override
    public CourseVO getCourseDetail(Long id) {
        Course course = baseMapper.selectById(id);
        if (course == null) {
            throw new RuntimeException("课程不存在");
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
        vo.setReviewCount(course.getReviewCount());

        if (course.getTeacherId() != null) {
            var teacher = teacherMapper.selectById(course.getTeacherId());
            if (teacher != null) {
                vo.setTeacherName(teacher.getTeacherName());
            }
        }

        return vo;
    }

    @Override
    public PageResult<CourseVO> searchCourses(CourseSearchRequest request) {
        int offset = (request.getPage() - 1) * request.getSize();
        List<CourseVO> courses = baseMapper.searchCourses(
                request.getKeyword(),
                request.getDepartment(),
                request.getMinScore(),
                request.getMaxScore(),
                offset,
                request.getSize()
        );
        long total = baseMapper.countSearch(
                request.getKeyword(),
                request.getDepartment(),
                request.getMinScore(),
                request.getMaxScore()
        );
        return new PageResult<>(courses, total, request.getPage(), request.getSize());
    }
}
