package com.siddharth.tradesim_backend.exchange.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record CreateExchangeRequest(
        @NotBlank(message = "Exchange name is required")
        @Size(max = 100, message = "Exchange name must not exceed 100 characters")
        String name,

        @NotBlank(message = "Exchange code is required")
        @Size(max = 10, message = "Exchange code must not exceed 10 characters")
        @Pattern(
                regexp = "^[A-Z0-9]+$",
                message = "Exchange code must contain only uppercase letters and numbers"
        )
        String code,

        @NotBlank(message = "Country is required")
        @Size(max = 60, message = "Country must not exceed 60 characters")
        String country,

        @NotBlank(message = "Timezone is required")
        @Size(max = 50, message = "Timezone must not exceed 50 characters")
        String timezone,

        @NotBlank(message = "Currency is required")
        @Size(max = 10, message = "Currency must not exceed 10 characters")
        @Pattern(
                regexp = "^[A-Z]{3,10}$",
                message = "Currency must contain only uppercase letters"
        )
        String currency,

        @NotNull(message = "Market open time is required")
        LocalTime marketOpenTime,

        @NotNull(message = "Market close time is required")
        LocalTime marketCloseTime
) {
}