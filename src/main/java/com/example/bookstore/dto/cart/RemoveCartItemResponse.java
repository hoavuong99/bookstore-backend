package com.example.bookstore.dto.cart;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RemoveCartItemResponse {
    private Long cartId;
    private Long itemId;
    private Integer remainingItems;
}
