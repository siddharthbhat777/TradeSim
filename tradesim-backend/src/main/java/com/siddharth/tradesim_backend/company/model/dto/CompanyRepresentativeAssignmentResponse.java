package com.siddharth.tradesim_backend.company.model.dto;

import com.siddharth.tradesim_backend.company.enums.CompanyRepresentativeAssignmentRole;
import com.siddharth.tradesim_backend.company.enums.CompanyRepresentativeAssignmentStatus;

import java.time.Instant;
import java.util.UUID;

public record CompanyRepresentativeAssignmentResponse(
        UUID id,
        UUID companyId,
        UUID userId,
        UUID assignedByUserId,
        CompanyRepresentativeAssignmentRole assignmentRole,
        CompanyRepresentativeAssignmentStatus status,
        Instant revokedAt,
        UUID revokedByUserId
) {
}