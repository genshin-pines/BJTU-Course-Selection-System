package com.bjtu.review.service;

import com.bjtu.review.dto.CourseSearchRequest;
import com.bjtu.review.vo.CourseVO;

import java.util.List;
import java.util.Map;

public interface CourseService {
    CourseVO getCourseInstanceDetail(Long instanceId);
    PageResult<CourseVO> searchCourses(CourseSearchRequest request);
    Map<String, List<String>> getFilterOptions();
}
