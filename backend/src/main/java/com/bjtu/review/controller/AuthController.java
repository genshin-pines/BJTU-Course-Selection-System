package com.bjtu.review.controller;

import com.bjtu.review.common.Result;
import com.bjtu.review.dto.LoginRequest;
import com.bjtu.review.dto.RegisterRequest;
import com.bjtu.review.dto.SendCodeRequest;
import com.bjtu.review.entity.Admin;
import com.bjtu.review.entity.Student;
import com.bjtu.review.mapper.AdminMapper;
import com.bjtu.review.service.StudentService;
import com.bjtu.review.service.VerifyCodeService;
import com.bjtu.review.vo.AuthSessionVO;
import com.bjtu.review.vo.LoginVO;
import com.bjtu.review.vo.RegisterVO;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final StudentService studentService;
    private final AdminMapper adminMapper;
    private final VerifyCodeService verifyCodeService;

    public AuthController(StudentService studentService, AdminMapper adminMapper, VerifyCodeService verifyCodeService) {
        this.studentService = studentService;
        this.adminMapper = adminMapper;
        this.verifyCodeService = verifyCodeService;
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        LoginVO vo = studentService.login(request);
        return Result.ok(vo);
    }

    @PostMapping("/register")
    public Result<RegisterVO> register(@Valid @RequestBody RegisterRequest request) {
        RegisterVO vo = studentService.register(request);
        return Result.ok(vo);
    }

    @PostMapping("/send-code")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeRequest request) {
        String email = request.getEmail();
        String emailStudentNo = email.substring(0, email.indexOf('@'));
        if (!emailStudentNo.equals(request.getStudentNo())) {
            return Result.fail("学号与邮箱前缀不匹配");
        }
        verifyCodeService.sendCode(email);
        return Result.ok();
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
            vo.setAdminRole(admin.getRole() == null || admin.getRole().isBlank() ? "SUPER_ADMIN" : admin.getRole());
        }
        return Result.ok(vo);
    }
}
