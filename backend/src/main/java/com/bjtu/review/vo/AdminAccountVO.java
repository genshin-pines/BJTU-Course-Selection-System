package com.bjtu.review.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminAccountVO {
    private Long id;
    private String username;
    private String role;
    private String department;
}
