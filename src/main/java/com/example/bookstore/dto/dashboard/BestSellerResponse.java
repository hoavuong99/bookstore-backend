package com.example.bookstore.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BestSellerResponse {
    private Long bookId;
    private String title;
    private Long quantitySold;
    private Integer stockQuantity;
}

