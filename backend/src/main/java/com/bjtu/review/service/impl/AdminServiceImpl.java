package com.bjtu.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bjtu.review.dto.AdminLoginRequest;
import com.bjtu.review.entity.Admin;
import com.bjtu.review.mapper.AdminMapper;
import com.bjtu.review.service.AdminService;
import com.bjtu.review.utils.JwtUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AdminServiceImpl(PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public String login(AdminLoginRequest request) {
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

        return jwtUtils.generateToken(admin.getId(), "ADMIN", admin.getUsername(), sessionId);
    }
}
