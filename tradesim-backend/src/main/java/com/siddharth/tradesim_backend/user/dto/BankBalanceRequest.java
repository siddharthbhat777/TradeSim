package com.siddharth.tradesim_backend.user.dto;

import jakarta.validation.constraints.NotBlank;

public record BankBalanceRequest(
        @NotBlank(message = "Password is required")
        String password
) {
}