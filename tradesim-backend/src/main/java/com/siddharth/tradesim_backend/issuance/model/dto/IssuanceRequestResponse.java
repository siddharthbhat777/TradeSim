package com.siddharth.tradesim_backend.issuance.model.dto;

import com.siddharth.tradesim_backend.issuance.enums.IssuanceStatus;

import java.time.Instant;
import java.util.UUID;

public record IssuanceRequestResponse(
        UUID id,
        UUID companyId,
        UUID stockId,
        UUID submittedByUserId,
        int totalIssuedShares,
        int tradableFloatShares,
        UUID liquidityProviderUserId,
        IssuanceStatus status,
        UUID reviewedByUserId,
        Instant reviewedAt,
        String rejectionReason,
        Instant createdAt,
        Instant updatedAt
) {
}