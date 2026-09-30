package com.example.bookstore.dto.order;

import com.example.bookstore.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutRequest {

    private String couponCode;

    @NotBlank
    private String shippingAddress;

    @NotBlank
    private String recipientName;

    @NotBlank
    private String recipientPhone;

    @NotNull
    private PaymentMethod paymentMethod;
}