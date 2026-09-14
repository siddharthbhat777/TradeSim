package com.siddharth.tradesim_backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EditProfileRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must not exceed 100 characters")
        String fullName,

        @NotBlank(message = "Linked bank name is required")
        @Size(max = 100, message = "Linked bank name must not exceed 100 characters")
        String linkedBankName
) {
}