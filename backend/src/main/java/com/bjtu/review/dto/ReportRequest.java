package com.bjtu.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportRequest {
    @NotNull(message = "评价ID不能为空")
    private Long reviewId;

    @NotBlank(message = "举报原因不能为空")
    private String reason;
}
