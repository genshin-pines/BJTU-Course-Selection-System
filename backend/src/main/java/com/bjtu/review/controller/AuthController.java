package com.bjtu.review.controller;

import com.bjtu.review.common.Result;
import com.bjtu.review.dto.LoginRequest;
import com.bjtu.review.entity.Admin;
import com.bjtu.review.entity.Student;
import com.bjtu.review.mapper.AdminMapper;
import com.bjtu.review.service.StudentService;
import com.bjtu.review.vo.AuthSessionVO;
import com.bjtu.review.vo.LoginVO;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final StudentService studentService;
    private final AdminMapper adminMapper;

    public AuthController(StudentService studentService, AdminMapper adminMapper) {
        this.studentService = studentService;
        this.adminMapper = adminMapper;
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        LoginVO vo = studentService.login(request);
        return Result.ok(vo);
    }

    @GetMapping("/session")
    public Result<AuthSessionVO> session(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");

        AuthSessionVO vo = new AuthSessionVO();
        vo.setUserId(userId);
        vo.setRole(role);

        if ("STUDENT".equals(role)) {
            Student student = studentService.getById(userId);
            vo.setUsername(student.getStudentNo());
            vo.setAnonymousId(student.getAnonymousId());
            vo.setName(student.getName());
        } else if ("ADMIN".equals(role)) {
            Admin admin = adminMapper.selectById(userId);
            vo.setUsername(admin.getUsername());
            vo.setName(admin.getUsername());
        }
        return Result.ok(vo);
    }
}
