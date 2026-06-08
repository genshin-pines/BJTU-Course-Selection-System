package com.bjtu.review.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bjtu.review.entity.Course;
import com.bjtu.review.vo.CourseVO;
import com.bjtu.review.dto.CourseSearchRequest;

public interface CourseService extends IService<Course> {
    CourseVO getCourseDetail(Long id);
    PageResult<CourseVO> searchCourses(CourseSearchRequest request);
}
