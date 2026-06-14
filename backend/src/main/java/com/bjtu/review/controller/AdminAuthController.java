package com.bjtu.review.controller;

import com.bjtu.review.common.Result;
import com.bjtu.review.dto.AdminLoginRequest;
import com.bjtu.review.service.AdminService;
import com.bjtu.review.vo.AdminLoginVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/admin")
public class AdminAuthController {

    private final AdminService adminService;

    public AdminAuthController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/login")
    public Result<AdminLoginVO> login(@Valid @RequestBody AdminLoginRequest request) {
        return Result.ok(adminService.login(request));
    }
}
