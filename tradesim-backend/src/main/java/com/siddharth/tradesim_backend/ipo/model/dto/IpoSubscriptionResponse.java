package com.siddharth.tradesim_backend.ipo.model.dto;

import com.siddharth.tradesim_backend.ipo.enums.IpoSubscriptionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record IpoSubscriptionResponse(
        UUID id,
        UUID ipoOfferId,
        UUID stockId,
        UUID userId,
        BigDecimal issuePrice,
        BigDecimal lockedAmount,
        int allottedShares,
        IpoSubscriptionStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}