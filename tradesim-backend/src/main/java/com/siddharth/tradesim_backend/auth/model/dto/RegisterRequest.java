package com.siddharth.tradesim_backend.auth.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must not exceed 100 characters")
        String fullName,

        @NotBlank(message = "Username is required")
        String username,

        @Email(message = "Invalid email format")
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Password is required")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*]).{8,}$",
                message = "Password must be at least 8 characters long and contain one uppercase letter, one number, and one special character"
        )
        String password,

        @NotBlank(message = "Linked bank name is required")
        @Size(max = 100, message = "Linked bank name must not exceed 100 characters")
        String linkedBankName,

        @NotBlank(message = "Country code is required (e.g., IN, US)")
        @Size(min = 2, max = 2, message = "Country code must be exactly 2 characters")
        @Pattern(regexp = "^[A-Z]{2}$", message = "Country code must be a valid 2-letter uppercase ISO code")
        String countryCode,

        @Pattern(regexp = "^[A-Z]{3}$", message = "Base currency must be a valid 3-letter ISO code")
        String baseCurrency
) {
}