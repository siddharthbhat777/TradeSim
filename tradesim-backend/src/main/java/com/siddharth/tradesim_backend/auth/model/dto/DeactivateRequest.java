package com.siddharth.tradesim_backend.auth.model.dto;

import jakarta.validation.constraints.NotBlank;

public record DeactivateRequest(
        @NotBlank(message = "Password is required")
        String password
) {
}