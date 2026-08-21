package com.siddharth.tradesim_backend.wallet.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CurrencyConversionRequest(
        @NotBlank String sourceCurrency,
        @NotBlank String targetCurrency,
        @NotNull @DecimalMin("0.01") BigDecimal amount
) {
}