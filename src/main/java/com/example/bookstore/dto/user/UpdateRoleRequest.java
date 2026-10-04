package com.example.bookstore.dto.user;

import com.example.bookstore.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRoleRequest {
    @NotNull
    private Role role;
}
