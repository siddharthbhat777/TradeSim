package com.siddharth.tradesim_backend.company.service;

import com.siddharth.tradesim_backend.auth.model.dto.RegisterResponse;
import com.siddharth.tradesim_backend.auth.service.AuthService;
import com.siddharth.tradesim_backend.company.model.dto.CompanyOnboardingResponse;
import com.siddharth.tradesim_backend.company.model.dto.CompanyRepresentativeAssignmentResponse;
import com.siddharth.tradesim_backend.company.model.dto.CompanyResponse;
import com.siddharth.tradesim_backend.company.model.dto.CreateCompanyOnboardingRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyOnboardingService {
    private final CompanyService companyService;
    private final AuthService authService;
    private final CompanyRepresentativeAssignmentService companyRepresentativeAssignmentService;

    @Transactional
    public CompanyOnboardingResponse onboardCompany(CreateCompanyOnboardingRequest request, UUID adminUserId) {
        CompanyResponse company = companyService.createCompany(request.company());
        RegisterResponse representative = authService.registerCompanyRepresentative(request.representative());
        CompanyRepresentativeAssignmentResponse assignment = companyRepresentativeAssignmentService.assignRepresentative(company.id(), representative.id(), adminUserId);

        return new CompanyOnboardingResponse(company, representative, assignment);
    }
}