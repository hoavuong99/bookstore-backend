package com.example.bookstore.dto.auth;

import com.example.bookstore.enums.Role;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private Long userId;
    private String email;
    private String fullName;
    private Role role;
}