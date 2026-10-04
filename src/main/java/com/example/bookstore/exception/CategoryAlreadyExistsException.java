package com.example.bookstore.exception;

public class CategoryAlreadyExistsException extends BusinessException {
    public CategoryAlreadyExistsException(String message) {
        super(message);
    }
}
