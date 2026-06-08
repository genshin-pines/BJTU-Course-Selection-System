package com.bjtu.review.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bjtu.review.dto.AdminLoginRequest;
import com.bjtu.review.entity.Admin;

public interface AdminService extends IService<Admin> {
    String login(AdminLoginRequest request);
}
