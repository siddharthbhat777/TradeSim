package com.siddharth.tradesim_backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*]).{8,}$",
                message = "Password must be at least 8 characters long and contain one uppercase letter, one number, and one special character"
        )
        String newPassword
) {
}