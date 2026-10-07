package com.example.bookstore.service;

import com.example.bookstore.dto.order.CheckoutRequest;
import com.example.bookstore.dto.order.CheckoutResponse;
import com.example.bookstore.dto.order.OrderDetailResponse;
import com.example.bookstore.dto.order.OrderSummaryResponse;
import com.example.bookstore.enums.OrderStatus;
import org.springframework.data.domain.Page;

public interface OrderService {
    CheckoutResponse checkout(Long userId, CheckoutRequest request);

    Page<OrderSummaryResponse> getMyOrders(Long userId, int page, int size);

    OrderDetailResponse getOrderById(Long requesterId, Long orderId, boolean isAdminOrStaff);

    Page<OrderSummaryResponse> getAllOrders(int page, int size);

    OrderDetailResponse updateOrderStatus(Long orderId, OrderStatus status);

    OrderDetailResponse cancelOrder(Long userId, Long orderId);
}