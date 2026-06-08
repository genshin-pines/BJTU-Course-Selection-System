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
        if (!passwordEncoder.matches(request.getPassword(), student.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        String token = jwtUtils.generateToken(student.getId(), "STUDENT", student.getStudentNo());
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
