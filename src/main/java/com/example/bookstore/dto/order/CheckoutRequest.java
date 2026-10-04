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

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    @NotBlank(message = "Receiver name is required")
    private String recipientName;

    @NotBlank(message = "Receiver phone is required")
    private String recipientPhone;

    @NotNull
    private PaymentMethod paymentMethod;
}