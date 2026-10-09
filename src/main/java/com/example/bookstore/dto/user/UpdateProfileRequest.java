package com.example.bookstore.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {
    @NotBlank
    private String fullName;
    @Pattern(regexp = "^$|^[0-9]{8,20}$", message = "Số điện thoại phải gồm 8-20 chữ số")
    private String phone;
    private String address;
}
