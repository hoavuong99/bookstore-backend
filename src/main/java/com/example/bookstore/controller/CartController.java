package com.example.bookstore.controller;

import com.example.bookstore.common.ApiResponse;
import com.example.bookstore.dto.cart.AddToCartRequest;
import com.example.bookstore.dto.cart.CartItemResponse;
import com.example.bookstore.dto.cart.CartResponse;
import com.example.bookstore.dto.cart.RemoveCartItemResponse;
import com.example.bookstore.security.CustomUserPrincipal;
import com.example.bookstore.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartItemResponse>> addBookToCart(
            Authentication authentication,
            @Valid @RequestBody AddToCartRequest request
    ) {
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
        CartItemResponse response = cartService.addBookToCart(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Book added to cart successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getMyCart(Authentication authentication) {
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
        CartResponse response = cartService.getCart(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Cart retrieved successfully", response));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<RemoveCartItemResponse>> removeCartItem(
            Authentication authentication,
            @PathVariable Long itemId
    ) {
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
        RemoveCartItemResponse response = cartService.removeCartItem(principal.getId(), itemId);
        return ResponseEntity.ok(ApiResponse.success("Cart item removed successfully", response));
    }
}
