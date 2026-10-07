package com.example.bookstore.controller;

import com.example.bookstore.common.ApiResponse;
import com.example.bookstore.dto.order.CheckoutRequest;
import com.example.bookstore.dto.order.CheckoutResponse;
import com.example.bookstore.dto.order.OrderDetailResponse;
import com.example.bookstore.dto.order.OrderSummaryResponse;
import com.example.bookstore.dto.order.PaymentResponse;
import com.example.bookstore.dto.order.UpdateOrderStatusRequest;
import com.example.bookstore.service.ZaloPayService;
import com.example.bookstore.service.OrderService;
import com.example.bookstore.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final ZaloPayService zaloPayService;

    @PostMapping("/{orderId}/payment/zalopay")
    public ResponseEntity<ApiResponse<PaymentResponse>> createZaloPayPayment(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        CustomUserPrincipal principal = getPrincipal(authentication);
        PaymentResponse response = zaloPayService.createSession(principal.getId(), orderId);
        return ResponseEntity.ok(ApiResponse.success("ZaloPay payment session created", response));
    }

    @GetMapping("/{orderId}/payment/zalopay")
    public ResponseEntity<ApiResponse<PaymentResponse>> refreshZaloPayPayment(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        CustomUserPrincipal principal = getPrincipal(authentication);
        PaymentResponse response = zaloPayService.refresh(principal.getId(), orderId);
        return ResponseEntity.ok(ApiResponse.success("ZaloPay payment status retrieved", response));
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(
            Authentication authentication,
            @Valid @RequestBody CheckoutRequest request
    ) {
        CustomUserPrincipal principal = getPrincipal(authentication);
        CheckoutResponse response = orderService.checkout(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Checkout completed successfully", response));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponse<Page<OrderSummaryResponse>>> getMyOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        CustomUserPrincipal principal = getPrincipal(authentication);
        Page<OrderSummaryResponse> response = orderService.getMyOrders(principal.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success("My orders retrieved successfully", response));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrderById(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        CustomUserPrincipal principal = getPrincipal(authentication);
        OrderDetailResponse response = orderService.getOrderById(
                principal.getId(),
                orderId,
                isAdminOrStaff(principal)
        );
        return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", response));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> cancelOrder(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        CustomUserPrincipal principal = getPrincipal(authentication);
        OrderDetailResponse response = orderService.cancelOrder(principal.getId(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", response));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<ApiResponse<Page<OrderSummaryResponse>>> getAllOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        CustomUserPrincipal principal = getPrincipal(authentication);
        ensureAdminOrStaff(principal);

        Page<OrderSummaryResponse> response = orderService.getAllOrders(page, size);
        return ResponseEntity.ok(ApiResponse.success("All orders retrieved successfully", response));
    }

    @PatchMapping("/admin/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> updateOrderStatus(
            @PathVariable Long orderId,
            Authentication authentication,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        CustomUserPrincipal principal = getPrincipal(authentication);
        ensureAdminOrStaff(principal);

        OrderDetailResponse response = orderService.updateOrderStatus(orderId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully", response));
    }

    private CustomUserPrincipal getPrincipal(Authentication authentication) {
        return (CustomUserPrincipal) authentication.getPrincipal();
    }

    private boolean isAdminOrStaff(CustomUserPrincipal principal) {
        return principal.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ROLE_STAFF"));
    }

    private void ensureAdminOrStaff(CustomUserPrincipal principal) {
        if (!isAdminOrStaff(principal)) {
            throw new AccessDeniedException("Access denied");
        }
    }
}