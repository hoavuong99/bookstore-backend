package com.example.bookstore.service;

import com.example.bookstore.dto.auth.AuthResponse;
import com.example.bookstore.dto.auth.LoginRequest;
import com.example.bookstore.dto.auth.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}