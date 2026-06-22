package com.bjtu.review.controller;

import com.bjtu.review.common.Result;
import com.bjtu.review.dto.CourseApplicationRequest;
import com.bjtu.review.service.CourseApplicationService;
import com.bjtu.review.vo.CourseApplicationVO;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/course-application")
public class CourseApplicationController {

    private final CourseApplicationService courseApplicationService;

    public CourseApplicationController(CourseApplicationService courseApplicationService) {
        this.courseApplicationService = courseApplicationService;
    }

    @PostMapping
    public Result<Long> submit(Authentication auth, @Valid @RequestBody CourseApplicationRequest request) {
        Long studentId = (Long) auth.getPrincipal();
        Long id = courseApplicationService.submitApplication(studentId, request);
        return Result.ok(id);
    }

    @GetMapping("/mine")
    public Result<List<CourseApplicationVO>> getMine(Authentication auth) {
        Long studentId = (Long) auth.getPrincipal();
        return Result.ok(courseApplicationService.getMyApplications(studentId));
    }
}
