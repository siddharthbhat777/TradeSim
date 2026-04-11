package com.siddharth.tradesim_backend.ipo.model.dto;

import com.siddharth.tradesim_backend.ipo.enums.IpoOfferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record IpoOfferResponse(
        UUID id,
        UUID companyId,
        UUID stockId,
        UUID submittedByUserId,
        BigDecimal issuePrice,
        int sharesPerAllottee,
        int maxAllottees,
        int totalSharesOffered,
        Instant subscriptionStartAt,
        Instant subscriptionEndAt,
        IpoOfferStatus status,
        UUID reviewedByUserId,
        Instant reviewedAt,
        UUID finalizedByUserId,
        Instant finalizedAt,
        String rejectionReason,
        Instant createdAt,
        Instant updatedAt
) {
}