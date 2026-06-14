package com.bjtu.review.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminLoginVO {
    private String token;
    private String adminRole;
}
