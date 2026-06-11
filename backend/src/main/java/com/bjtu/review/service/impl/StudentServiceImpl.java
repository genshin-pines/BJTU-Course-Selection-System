package com.bjtu.review.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bjtu.review.dto.LoginRequest;
import com.bjtu.review.entity.Student;
import com.bjtu.review.mapper.StudentMapper;
import com.bjtu.review.service.StudentService;
import com.bjtu.review.utils.JwtUtils;
import com.bjtu.review.vo.LoginVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class StudentServiceImpl extends ServiceImpl<StudentMapper, Student> implements StudentService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public StudentServiceImpl(PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public LoginVO login(LoginRequest request) {
        Student student = baseMapper.selectByStudentNo(request.getStudentNo());
        if (student == null) {
            throw new RuntimeException("学号不存在");
        }

        boolean matched = false;
        // 先尝试 BCrypt 匹配
        try {
            matched = passwordEncoder.matches(request.getPassword(), student.getPassword());
        } catch (Exception ignored) {
        }
        // BCrypt 失败则尝试明文匹配（兜底，同时自动升级为 BCrypt）
        if (!matched && request.getPassword().equals(student.getPassword())) {
            matched = true;
            // 自动升级密码为 BCrypt 哈希
            student.setPassword(passwordEncoder.encode(request.getPassword()));
            baseMapper.updateById(student);
        }
        if (!matched) {
            throw new RuntimeException("密码错误");
        }
        String sessionId = UUID.randomUUID().toString();
        student.setCurrentSessionId(sessionId);
        baseMapper.updateById(student);

        String token = jwtUtils.generateToken(student.getId(), "STUDENT", student.getStudentNo(), sessionId);
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setAnonymousId(student.getAnonymousId());
        vo.setStudentNo(student.getStudentNo());
        vo.setName(student.getName());
        return vo;
    }

    @Override
    public Student getById(Long id) {
        return baseMapper.selectById(id);
    }
}
