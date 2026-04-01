package com.siddharth.tradesim_backend.company.model.dto;

import java.time.Instant;
import java.util.UUID;

public record PrimaryContactTransferResponse(
        UUID companyId,
        UUID previousPrimaryContactUserId,
        UUID newPrimaryContactUserId,
        UUID changedByUserId,
        Instant transferredAt
) {
}