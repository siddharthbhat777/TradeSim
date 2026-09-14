package com.siddharth.tradesim_backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerifyEmailChangeRequest(
        @NotBlank(message = "New email is required")
        @Email(message = "Invalid email format")
        String newEmail,

        @NotBlank(message = "OTP is required")
        String otp
) {
}