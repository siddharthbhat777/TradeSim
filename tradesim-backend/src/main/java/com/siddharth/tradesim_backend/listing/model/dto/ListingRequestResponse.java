package com.siddharth.tradesim_backend.listing.model.dto;

import com.siddharth.tradesim_backend.listing.enums.ListingStatus;
import com.siddharth.tradesim_backend.stock.enums.Sector;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ListingRequestResponse(
        UUID id,
        UUID companyId,
        String companyName,
        UUID submittedByUserId,
        String symbol,
        UUID exchangeId,
        String exchangeName,
        BigDecimal referencePrice,
        Sector sector,
        BigDecimal priceBandPercent,
        Integer totalShares,
        List<CapTableEntryResponse> capTable,
        ListingStatus status,
        UUID reviewedByUserId,
        Instant reviewedAt,
        UUID approvedStockId,
        String rejectionReason,
        String currency,
        Instant createdAt,
        Instant updatedAt
) {
}