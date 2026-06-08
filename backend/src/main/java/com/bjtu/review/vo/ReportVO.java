package com.bjtu.review.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReportVO {
    private Long id;
    private Long reviewId;
    private String reviewContent;
    private Long reporterId;
    private String reason;
    private String status;
    private LocalDateTime createTime;
}
