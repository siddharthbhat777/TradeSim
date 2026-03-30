package com.siddharth.tradesim_backend.stock.model.dto;

import com.siddharth.tradesim_backend.stock.enums.Sector;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateStockRequest(
        @NotBlank(message = "Stock symbol is required")
        @Size(max = 10, message = "Symbol must not exceed 10 characters")
        @Pattern(
                regexp = "^[A-Z0-9]+$",
                message = "Symbol must contain only uppercase letters and numbers"
        )
        String symbol,

        @NotNull(message = "Company ID is required")
        UUID companyId,

        @NotNull(message = "Exchange ID is required")
        UUID exchangeId,

        @NotNull(message = "Initial price is required")
        @DecimalMin(
                value = "0.01",
                inclusive = true,
                message = "Price must be greater than zero"
        )
        @Digits(
                integer = 15,
                fraction = 4,
                message = "Price must have up to 4 decimal places"
        )
        BigDecimal initialPrice,

        @NotNull(message = "Sector is required")
        Sector sector,

        @DecimalMin(value = "0.1")
        @DecimalMax(value = "50")
        @Digits(integer = 3, fraction = 2)
        BigDecimal priceBandPercent
) {
}