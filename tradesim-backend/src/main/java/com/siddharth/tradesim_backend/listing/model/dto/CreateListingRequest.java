package com.siddharth.tradesim_backend.listing.model.dto;

import com.siddharth.tradesim_backend.stock.enums.Sector;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateListingRequest(
        @NotBlank(message = "Stock symbol is required")
        @Size(max = 10, message = "Symbol must not exceed 10 characters")
        @Pattern(
                regexp = "^[A-Z0-9]+$",
                message = "Symbol must contain only uppercase letters and numbers"
        )
        String symbol,

        @NotNull(message = "Exchange ID is required")
        UUID exchangeId,

        @NotNull(message = "Reference price is required")
        @DecimalMin(
                value = "0.01",
                message = "Reference price must be greater than zero"
        )
        @Digits(
                integer = 15,
                fraction = 4,
                message = "Reference price must have up to 4 decimal places"
        )
        BigDecimal referencePrice,

        @NotNull(message = "Sector is required")
        Sector sector,

        @DecimalMin(value = "0.1", message = "Price band percent must be at least 0.1")
        @DecimalMax(value = "50", message = "Price band percent must not exceed 50")
        @Digits(integer = 3, fraction = 2, message = "Price band percent must have up to 2 decimal places")
        BigDecimal priceBandPercent
) {
}