package com.siddharth.tradesim_backend.company.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCompanyRequest(
        @NotBlank(message = "Company name is required")
        @Size(max = 120, message = "Company name must not exceed 120 characters")
        String name,

        @NotBlank(message = "Company code is required")
        @Size(max = 20, message = "Company code must not exceed 20 characters")
        @Pattern(
                regexp = "^[A-Z0-9]+$",
                message = "Company code must contain only uppercase letters and numbers"
        )
        String code,

        @NotBlank(message = "Country is required")
        @Size(max = 60, message = "Country must not exceed 60 characters")
        String country
) {
}