package com.example.bookstore.exception;

public class BookAlreadyExistsException extends BusinessException {
    public BookAlreadyExistsException(String message) {
        super(message);
    }
}
