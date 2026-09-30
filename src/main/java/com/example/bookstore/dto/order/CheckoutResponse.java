package com.example.bookstore.dto.order;

import com.example.bookstore.enums.OrderStatus;
import com.example.bookstore.enums.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CheckoutResponse {
    private Long orderId;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
}