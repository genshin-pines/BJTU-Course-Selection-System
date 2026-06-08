package com.bjtu.review.vo;

import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private String anonymousId;
    private String studentNo;
    private String name;
}
