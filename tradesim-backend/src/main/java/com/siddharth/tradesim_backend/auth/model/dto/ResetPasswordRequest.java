package com.siddharth.tradesim_backend.auth.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "OTP is required")
        String otp,

        @NotBlank(message = "New password is required")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*]).{8,}$",
                message = "Password must be at least 8 characters long and contain one uppercase letter, one number, and one special character"
        )
        String newPassword
) {
}