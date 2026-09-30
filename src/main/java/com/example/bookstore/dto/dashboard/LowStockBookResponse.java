package com.example.bookstore.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class LowStockBookResponse {
    private Long bookId;
    private String title;
    private String isbn;
    private Integer stockQuantity;
    private BigDecimal price;
}
