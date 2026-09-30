package com.example.bookstore.service.impl;

import com.example.bookstore.dto.auth.AuthResponse;
import com.example.bookstore.dto.auth.LoginRequest;
import com.example.bookstore.dto.auth.RegisterRequest;
import com.example.bookstore.service.AuthService;
import com.example.bookstore.exception.EmailAlreadyExistsException;
import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.entity.User;
import com.example.bookstore.enums.Role;
import com.example.bookstore.repository.UserRepository;
import com.example.bookstore.security.CustomUserPrincipal;
import com.example.bookstore.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        userRepository.findByEmail(request.getEmail())
                .ifPresent(user -> {
                    throw new EmailAlreadyExistsException("Email is already in use");
                });

        User user = new User();
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setRole(Role.CUSTOMER);
        user.setIsActive(true);

        User savedUser = userRepository.save(user);
        CustomUserPrincipal principal = new CustomUserPrincipal(savedUser);
        String accessToken = jwtService.generateToken(principal);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .role(savedUser.getRole())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CustomUserPrincipal principal = new CustomUserPrincipal(user);
        String accessToken = jwtService.generateToken(principal);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }
}