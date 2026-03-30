package com.siddharth.tradesim_backend.company.model.dto;

import com.siddharth.tradesim_backend.company.enums.CompanyStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeCompanyStatusRequest(
        @NotNull(message = "Status is required")
        CompanyStatus status
) {
}