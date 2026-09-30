package com.example.bookstore.dto.order;

import com.example.bookstore.enums.OrderStatus;
import com.example.bookstore.enums.PaymentMethod;
import com.example.bookstore.enums.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderDetailResponse {
    private Long orderId;
    private Long userId;
    private String shippingAddress;
    private String recipientName;
    private String recipientPhone;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private OrderStatus orderStatus;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;
}
