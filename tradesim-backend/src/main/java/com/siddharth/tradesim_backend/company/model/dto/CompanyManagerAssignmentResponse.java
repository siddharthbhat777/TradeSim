package com.siddharth.tradesim_backend.company.model.dto;

import com.siddharth.tradesim_backend.company.enums.CompanyManagerAssignmentStatus;

import java.time.Instant;
import java.util.UUID;

public record CompanyManagerAssignmentResponse(
        UUID id,
        UUID companyId,
        UUID userId,
        UUID assignedByAdminId,
        CompanyManagerAssignmentStatus status,
        Instant revokedAt
) {
}