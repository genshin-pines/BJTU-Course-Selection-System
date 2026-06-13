package com.bjtu.review.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bjtu.review.entity.Course;
import com.bjtu.review.vo.CourseInstanceVO;
import com.bjtu.review.vo.CourseVO;
import com.bjtu.review.dto.CourseSearchRequest;

import java.util.List;

public interface CourseService extends IService<Course> {
    CourseVO getCourseDetail(Long id);
    CourseVO getCourseInstanceDetail(Long instanceId);
    PageResult<CourseVO> searchCourses(CourseSearchRequest request);
    List<CourseInstanceVO> getCourseInstances(Long id);
}
