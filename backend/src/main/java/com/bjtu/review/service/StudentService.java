package com.bjtu.review.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bjtu.review.dto.LoginRequest;
import com.bjtu.review.dto.RegisterRequest;
import com.bjtu.review.entity.Student;
import com.bjtu.review.vo.LoginVO;
import com.bjtu.review.vo.RegisterVO;

public interface StudentService extends IService<Student> {
    LoginVO login(LoginRequest request);
    RegisterVO register(RegisterRequest request);
    Student getById(Long id);
}
