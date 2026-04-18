package com.siddharth.tradesim_backend.user.dto;

import com.siddharth.tradesim_backend.auth.enums.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeUserRoleRequest(
        @NotNull(message = "Role is required")
        Role role
) {
}