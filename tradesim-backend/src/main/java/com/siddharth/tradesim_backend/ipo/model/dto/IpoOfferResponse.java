package com.siddharth.tradesim_backend.ipo.model.dto;

import com.siddharth.tradesim_backend.ipo.enums.IpoOfferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record IpoOfferResponse(
        UUID id,
        UUID companyId,
        String companyName,
        UUID stockId,
        String symbol,
        UUID submittedByUserId,
        BigDecimal issuePrice,
        Integer sharesPerAllottee,
        Integer maxAllottees,
        Integer totalSharesOffered,
        Instant subscriptionStartAt,
        Instant subscriptionEndAt,
        IpoOfferStatus status,
        UUID reviewedByUserId,
        Instant reviewedAt,
        UUID finalizedByUserId,
        Instant finalizedAt,
        String rejectionReason,
        String exchangeName,
        String currency,
        Instant createdAt,
        Instant updatedAt
) {
}