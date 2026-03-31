package com.siddharth.tradesim_backend.company.model.dto;

import com.siddharth.tradesim_backend.company.enums.CompanyRepresentativeAssignmentStatus;

import java.time.Instant;
import java.util.UUID;

public record CompanyRepresentativeAssignmentResponse(
        UUID id,
        UUID companyId,
        UUID userId,
        UUID assignedByAdminId,
        CompanyRepresentativeAssignmentStatus status,
        Instant revokedAt
) {
}