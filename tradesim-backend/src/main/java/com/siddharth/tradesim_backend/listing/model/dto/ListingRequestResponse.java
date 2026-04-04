package com.siddharth.tradesim_backend.listing.model.dto;

import com.siddharth.tradesim_backend.listing.enums.ListingStatus;
import com.siddharth.tradesim_backend.stock.enums.Sector;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ListingRequestResponse(
        UUID id,
        UUID companyId,
        UUID submittedByUserId,
        String symbol,
        UUID exchangeId,
        BigDecimal referencePrice,
        Sector sector,
        BigDecimal priceBandPercent,
        ListingStatus status,
        UUID reviewedByUserId,
        Instant reviewedAt,
        UUID approvedStockId,
        String rejectionReason,
        Instant createdAt,
        Instant updatedAt
) {
}