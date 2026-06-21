package com.bjtu.review.controller;

import com.bjtu.review.common.Result;
import com.bjtu.review.dto.CourseSearchRequest;
import com.bjtu.review.service.CourseService;
import com.bjtu.review.service.PageResult;
import com.bjtu.review.vo.CourseVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/course")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/search")
    public Result<PageResult<CourseVO>> search(CourseSearchRequest request) {
        PageResult<CourseVO> result = courseService.searchCourses(request);
        return Result.ok(result);
    }

    @GetMapping("/instances/{instanceId}/detail")
    public Result<CourseVO> getInstanceDetail(@PathVariable Long instanceId) {
        return Result.ok(courseService.getCourseInstanceDetail(instanceId));
    }

    @GetMapping("/filters/options")
    public Result<Map<String, List<String>>> getFilterOptions() {
        return Result.ok(courseService.getFilterOptions());
    }
}
