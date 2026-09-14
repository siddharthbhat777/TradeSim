package com.siddharth.tradesim_backend.wallet.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CurrencyConversionRequest(
        @NotBlank(message = "Source currency code is required")
        @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters")
        String sourceCurrencyCode,

        @NotBlank(message = "Target currency code is required")
        @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters")
        String targetCurrencyCode,

        @NotNull(message = "Amount to convert is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amountToConvert
) {
}