package com.siddharth.tradesim_backend.auth.model.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Username or E-Mail is required")
        String usernameOrEmail,

        @NotBlank(message = "Password is required")
        String password
) {
}