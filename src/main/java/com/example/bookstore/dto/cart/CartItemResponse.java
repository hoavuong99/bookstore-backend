package com.example.bookstore.dto.cart;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CartItemResponse {
    private Long cartId;
    private Long bookId;
    private String bookTitle;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal lineTotal;
}
