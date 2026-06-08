package com.bjtu.review;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.bjtu.review.mapper")
public class BjtuReviewApplication {
    public static void main(String[] args) {
        SpringApplication.run(BjtuReviewApplication.class, args);
    }
}
