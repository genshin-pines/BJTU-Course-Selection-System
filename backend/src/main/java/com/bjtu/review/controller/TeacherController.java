package com.bjtu.review.controller;

import com.bjtu.review.common.Result;
import com.bjtu.review.service.TeacherService;
import com.bjtu.review.vo.TeacherVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @GetMapping
    public Result<List<TeacherVO>> getAllTeachers() {
        return Result.ok(teacherService.getAllTeachers());
    }

    @GetMapping("/{id}")
    public Result<TeacherVO> getDetail(@PathVariable Long id) {
        return Result.ok(teacherService.getTeacherDetail(id));
    }
}
