package com.example.bookstore.dto.order;

import com.example.bookstore.enums.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentResponse {
    private Long orderId;
    private String provider;
    private PaymentStatus paymentStatus;
    private String state;
    private String paymentUrl;
    private LocalDateTime expiresAt;
    private String error;
}
