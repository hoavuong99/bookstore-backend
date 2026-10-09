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
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*[0-9]).{8,128}$",
            message = "Mật khẩu phải có ít nhất 8 ký tự, gồm cả chữ và số"
    )
    private String password;

    @NotBlank
    @Size(max = 255)
    private String fullName;

    @Pattern(regexp = "^$|^[0-9]{8,20}$", message = "Số điện thoại phải gồm 8-20 chữ số")
    private String phone;

    @Size(max = 500)
    private String address;
}