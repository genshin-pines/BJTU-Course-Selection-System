package com.bjtu.review.vo;

import lombok.Data;

@Data
public class AuthSessionVO {
    private Long userId;
    private String role;
    private String username;
    private String anonymousId;
    private String name;
}
