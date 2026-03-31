package com.siddharth.tradesim_backend.company.model.dto;

import com.siddharth.tradesim_backend.auth.model.dto.RegisterResponse;

public record CompanyOnboardingResponse(
        CompanyResponse company,
        RegisterResponse manager,
        CompanyManagerAssignmentResponse assignment
) {
}