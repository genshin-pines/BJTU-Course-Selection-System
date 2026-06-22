package com.bjtu.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "邮箱不能为空")
    @Pattern(regexp = "^\\d{8}@bjtu\\.edu\\.cn$", message = "邮箱格式必须为8位学号@bjtu.edu.cn")
    private String email;

    @NotBlank(message = "学号不能为空")
    @Pattern(regexp = "^\\d{8}$", message = "学号必须为8位数字")
    private String studentNo;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "验证码不能为空")
    private String code;
}
