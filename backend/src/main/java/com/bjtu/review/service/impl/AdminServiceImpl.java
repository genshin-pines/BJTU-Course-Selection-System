package com.bjtu.review.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bjtu.review.dto.AdminLoginRequest;
import com.bjtu.review.entity.Admin;
import com.bjtu.review.mapper.AdminMapper;
import com.bjtu.review.service.AdminService;
import com.bjtu.review.utils.JwtUtils;
import com.bjtu.review.vo.AdminLoginVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    private static final String DEFAULT_ADMIN_ROLE = "SUPER_ADMIN";

    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AdminServiceImpl(PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public AdminLoginVO login(AdminLoginRequest request) {
        Admin admin = baseMapper.selectByUsername(request.getUsername());
        if (admin == null) {
            throw new RuntimeException("管理员不存在");
        }
        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        String sessionId = UUID.randomUUID().toString();
        admin.setCurrentSessionId(sessionId);
        baseMapper.updateById(admin);

        String adminRole = normalizeRole(admin.getRole());
        String token = jwtUtils.generateToken(admin.getId(), "ADMIN", admin.getUsername(), sessionId);
        return new AdminLoginVO(token, adminRole);
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return DEFAULT_ADMIN_ROLE;
        }
        return role;
    }
}
