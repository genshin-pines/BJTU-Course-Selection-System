package com.bjtu.review.service.impl;

import com.bjtu.review.dto.CourseSearchRequest;
import com.bjtu.review.mapper.CourseInstanceMapper;
import com.bjtu.review.mapper.CourseMapper;
import com.bjtu.review.service.CourseService;
import com.bjtu.review.service.PageResult;
import com.bjtu.review.vo.CourseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CourseServiceImpl implements CourseService {

    private final CourseMapper courseMapper;
    private final CourseInstanceMapper courseInstanceMapper;

    public CourseServiceImpl(CourseMapper courseMapper, CourseInstanceMapper courseInstanceMapper) {
        this.courseMapper = courseMapper;
        this.courseInstanceMapper = courseInstanceMapper;
    }

    @Override
    public CourseVO getCourseInstanceDetail(Long instanceId) {
        CourseVO vo = courseInstanceMapper.selectDetailByInstanceId(instanceId);
        if (vo == null) {
            throw new RuntimeException("开课实例不存在");
        }
        return vo;
    }

    @Override
    public PageResult<CourseVO> searchCourses(CourseSearchRequest request) {
        int page = request.getPage() == null || request.getPage() < 1 ? 1 : request.getPage();
        int size = request.getSize() == null || request.getSize() < 1 ? 10 : Math.min(request.getSize(), 50);
        int offset = (page - 1) * size;
        List<CourseVO> courses = courseMapper.searchCourses(
                request.getKeyword(),
                request.getDepartment(),
                request.getTeacherName(),
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
                request.getTagMatchMode(),
                request.getSortBy(),
                request.getSortOrder(),
                offset,
                size
        );
        long total = courseMapper.countSearch(
                request.getKeyword(),
                request.getDepartment(),
                request.getTeacherName(),
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
    public Map<String, List<String>> getFilterOptions() {
        Map<String, List<String>> options = new java.util.HashMap<>();
        options.put("departments", safeLoadFilterOptions("departments", courseMapper::selectDistinctDepartments));
        options.put("teachers", safeLoadFilterOptions("teachers", courseMapper::selectDistinctTeachers));
        return options;
    }

    private List<String> safeLoadFilterOptions(String name, java.util.function.Supplier<List<String>> supplier) {
        try {
            List<String> values = supplier.get();
            return values == null ? List.of() : values;
        } catch (Exception e) {
            log.error("Failed to load course filter option '{}'", name, e);
            return List.of();
        }
    }
}
