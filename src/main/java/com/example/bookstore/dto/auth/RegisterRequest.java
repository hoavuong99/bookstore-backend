package com.example.bookstore.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 8, max = 128)
    private String password;

    @NotBlank
    @Size(max = 255)
    private String fullName;

    @Pattern(regexp = "^$|^[0-9+\\-() ]{8,20}$", message = "Phone number is invalid")
    private String phone;

    @Size(max = 500)
    private String address;
}