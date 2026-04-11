package com.siddharth.tradesim_backend.ipo.model.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateIpoOfferRequest(
        @NotNull(message = "Issue price is required")
        @DecimalMin(value = "0.01", message = "Issue price must be greater than zero")
        @Digits(integer = 15, fraction = 4, message = "Issue price must have up to 4 decimal places")
        BigDecimal issuePrice,

        @Min(value = 1, message = "Shares per allottee must be at least 1")
        int sharesPerAllottee,

        @Min(value = 1, message = "Max allottees must be at least 1")
        int maxAllottees,

        @NotNull(message = "Subscription start time is required")
        Instant subscriptionStartAt,

        @NotNull(message = "Subscription end time is required")
        Instant subscriptionEndAt
) {
}