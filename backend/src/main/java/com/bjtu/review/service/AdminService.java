package com.bjtu.review.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bjtu.review.dto.AdminLoginRequest;
import com.bjtu.review.entity.Admin;
import com.bjtu.review.vo.AdminLoginVO;

public interface AdminService extends IService<Admin> {
    AdminLoginVO login(AdminLoginRequest request);
}
