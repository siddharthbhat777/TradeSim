package com.siddharth.tradesim_backend.issuance.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateIssuanceRequest(
        @Min(value = 1, message = "Total issued shares must be at least 1")
        int totalIssuedShares,

        @Min(value = 1, message = "Tradable float shares must be at least 1")
        int tradableFloatShares,

        @NotNull(message = "Liquidity provider user ID is required")
        UUID liquidityProviderUserId
) {
}