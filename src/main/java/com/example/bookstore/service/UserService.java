package com.example.bookstore.service;

import com.example.bookstore.dto.user.ChangePasswordRequest;
import com.example.bookstore.dto.user.UpdateProfileRequest;
import com.example.bookstore.dto.user.UserResponse;
import com.example.bookstore.enums.Role;
import org.springframework.data.domain.Page;

public interface UserService {
    UserResponse getUser(Long userId);

    UserResponse updateProfile(Long userId, UpdateProfileRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    Page<UserResponse> getUsers(int page, int size);

    UserResponse updateRole(Long userId, Role role);
}
