package com.siddharth.tradesim_backend.auth.model.dto;

import com.siddharth.tradesim_backend.auth.enums.OtpPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendOtpRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotNull(message = "Purpose is required")
        OtpPurpose purpose
) {
}