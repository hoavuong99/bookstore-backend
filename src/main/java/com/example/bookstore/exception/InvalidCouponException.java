package com.example.bookstore.exception;

public class InvalidCouponException extends BusinessException {
    public InvalidCouponException(String message) {
        super(message);
    }
}