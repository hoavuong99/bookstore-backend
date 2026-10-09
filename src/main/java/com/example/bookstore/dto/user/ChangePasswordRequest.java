package com.example.bookstore.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {
    @NotBlank
    private String currentPassword;
    @NotBlank
    @Size(min = 8, max = 128)
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*[0-9]).{8,128}$",
            message = "Mật khẩu phải có ít nhất 8 ký tự, gồm cả chữ và số"
    )
    private String newPassword;
}
