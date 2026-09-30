package com.example.bookstore.dto.cart;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class CartResponse {
    private Long cartId;
    private Integer totalItems;
    private BigDecimal subtotal;
    private List<CartItemResponse> items;
}
