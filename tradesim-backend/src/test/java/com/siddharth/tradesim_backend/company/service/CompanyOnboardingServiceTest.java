package com.siddharth.tradesim_backend.company.service;

import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.dto.RegisterRequest;
import com.siddharth.tradesim_backend.auth.model.dto.RegisterResponse;
import com.siddharth.tradesim_backend.auth.service.AuthService;
import com.siddharth.tradesim_backend.company.enums.CompanyManagerAssignmentStatus;
import com.siddharth.tradesim_backend.company.enums.CompanyStatus;
import com.siddharth.tradesim_backend.company.model.dto.CompanyManagerAssignmentResponse;
import com.siddharth.tradesim_backend.company.model.dto.CompanyOnboardingResponse;
import com.siddharth.tradesim_backend.company.model.dto.CompanyResponse;
import com.siddharth.tradesim_backend.company.model.dto.CreateCompanyOnboardingRequest;
import com.siddharth.tradesim_backend.company.model.dto.CreateCompanyRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyOnboardingServiceTest {

    @Mock
    private CompanyService companyService;

    @Mock
    private AuthService authService;

    @Mock
    private CompanyManagerAssignmentService companyManagerAssignmentService;

    @InjectMocks
    private CompanyOnboardingService companyOnboardingService;

    @Test
    void shouldOnboardCompanyWithInitialManager() {
        UUID adminUserId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        CreateCompanyRequest companyRequest = new CreateCompanyRequest(
                "Apple Inc",
                "APPLE",
                "United States"
        );

        RegisterRequest managerRequest = new RegisterRequest(
                "apple_manager",
                "apple_manager@example.com",
                "Manager@123"
        );

        CreateCompanyOnboardingRequest request = new CreateCompanyOnboardingRequest(companyRequest, managerRequest);

        CompanyResponse companyResponse = new CompanyResponse(
                companyId,
                "Apple Inc",
                "APPLE",
                "United States",
                CompanyStatus.ACTIVE
        );

        RegisterResponse managerResponse = new RegisterResponse(
                managerId,
                "apple_manager",
                "apple_manager@example.com",
                Role.COMPANY_MANAGER,
                AccountStatus.ACTIVE
        );

        CompanyManagerAssignmentResponse assignmentResponse = new CompanyManagerAssignmentResponse(
                UUID.randomUUID(),
                companyId,
                managerId,
                adminUserId,
                CompanyManagerAssignmentStatus.ACTIVE,
                null
        );

        when(companyService.createCompany(eq(companyRequest))).thenReturn(companyResponse);
        when(authService.registerCompanyManager(eq(managerRequest))).thenReturn(managerResponse);
        when(companyManagerAssignmentService.assignManager(eq(companyId), eq(managerId), eq(adminUserId))).thenReturn(assignmentResponse);

        CompanyOnboardingResponse response = companyOnboardingService.onboardCompany(request, adminUserId);

        assertThat(response.company().id()).isEqualTo(companyId);
        assertThat(response.manager().id()).isEqualTo(managerId);
        assertThat(response.assignment().status()).isEqualTo(CompanyManagerAssignmentStatus.ACTIVE);
    }
}