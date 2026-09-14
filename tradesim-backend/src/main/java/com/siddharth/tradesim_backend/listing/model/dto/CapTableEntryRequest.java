package com.siddharth.tradesim_backend.listing.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CapTableEntryRequest(
        @NotNull(message = "User ID is required")
        UUID userId,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity
) {
}