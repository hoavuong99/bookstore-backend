package com.example.bookstore.dto.order;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class OrderItemResponse {
    private Long bookId;
    private String bookTitle;
    private String imageUrl;
    private Integer quantity;
    private BigDecimal price;
}
