package com.bjtu.review.controller;

import com.bjtu.review.common.Result;
import com.bjtu.review.dto.AdminLoginRequest;
import com.bjtu.review.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/admin")
public class AdminAuthController {

    private final AdminService adminService;

    public AdminAuthController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/login")
    public Result<Map<String, String>> login(@Valid @RequestBody AdminLoginRequest request) {
        String token = adminService.login(request);
        return Result.ok(Map.of("token", token));
    }
}
