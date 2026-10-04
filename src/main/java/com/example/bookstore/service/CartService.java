package com.example.bookstore.service;

import com.example.bookstore.dto.cart.AddToCartRequest;
import com.example.bookstore.dto.cart.CartItemResponse;
import com.example.bookstore.dto.cart.CartResponse;
import com.example.bookstore.dto.cart.RemoveCartItemResponse;

public interface CartService {
    CartItemResponse addBookToCart(Long userId, AddToCartRequest request);

    CartResponse getCart(Long userId);

    RemoveCartItemResponse removeCartItem(Long userId, Long itemId);

    CartResponse clearCart(Long userId);
}
