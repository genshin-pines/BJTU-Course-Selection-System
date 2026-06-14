package com.bjtu.review.dto;

import lombok.Data;

@Data
public class AdminCreateRequest {
    private String username;
    private String password;
    private String role;
    private String department;
}
