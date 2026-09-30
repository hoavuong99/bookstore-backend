package com.example.bookstore.exception;

public class BadRequestException extends BusinessException {
    public BadRequestException(String message) {
        super(message);
    }
}
