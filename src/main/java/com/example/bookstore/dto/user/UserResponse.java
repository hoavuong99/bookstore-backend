package com.example.bookstore.dto.user;

import com.example.bookstore.enums.Role;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String address;
    private Role role;
    private Boolean isActive;
}
