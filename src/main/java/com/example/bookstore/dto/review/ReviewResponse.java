package com.example.bookstore.dto.review;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReviewResponse {
    private Long id;
    private Long userId;
    private String customerName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
