package com.bjtu.review.controller;

import com.bjtu.review.common.Result;
import com.bjtu.review.dto.LoginRequest;
import com.bjtu.review.service.StudentService;
import com.bjtu.review.vo.LoginVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final StudentService studentService;

    public AuthController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        LoginVO vo = studentService.login(request);
        return Result.ok(vo);
    }
}
