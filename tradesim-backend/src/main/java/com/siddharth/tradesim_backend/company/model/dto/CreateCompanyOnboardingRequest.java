package com.siddharth.tradesim_backend.company.model.dto;

import com.siddharth.tradesim_backend.auth.model.dto.RegisterRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CreateCompanyOnboardingRequest(
        @NotNull(message = "Company details are required")
        @Valid
        CreateCompanyRequest company,

        @NotNull(message = "Manager details are required")
        @Valid
        RegisterRequest manager
) {
}