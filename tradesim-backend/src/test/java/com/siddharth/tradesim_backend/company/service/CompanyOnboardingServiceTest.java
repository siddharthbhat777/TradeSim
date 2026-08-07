package com.siddharth.tradesim_backend.company.service;

import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.dto.RegisterRequest;
import com.siddharth.tradesim_backend.auth.model.dto.RegisterResponse;
import com.siddharth.tradesim_backend.auth.service.AuthService;
import com.siddharth.tradesim_backend.company.enums.CompanyRepresentativeAssignmentRole;
import com.siddharth.tradesim_backend.company.enums.CompanyRepresentativeAssignmentStatus;
import com.siddharth.tradesim_backend.company.enums.CompanyStatus;
import com.siddharth.tradesim_backend.company.model.dto.CompanyOnboardingResponse;
import com.siddharth.tradesim_backend.company.model.dto.CompanyRepresentativeAssignmentResponse;
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
    private CompanyRepresentativeAssignmentService companyRepresentativeAssignmentService;

    @InjectMocks
    private CompanyOnboardingService companyOnboardingService;

    @Test
    void shouldOnboardCompanyWithInitialPrimaryContact() {
        UUID adminUserId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID representativeId = UUID.randomUUID();

        CreateCompanyRequest companyRequest = new CreateCompanyRequest("Apple Inc", "APPLE", "United States");
        RegisterRequest representativeRequest = new RegisterRequest("apple_representative", "apple_representative@example.com", "Representative@123", "US");
        CreateCompanyOnboardingRequest request = new CreateCompanyOnboardingRequest(companyRequest, representativeRequest);

        CompanyResponse companyResponse = new CompanyResponse(companyId, "Apple Inc", "APPLE", "United States", CompanyStatus.ACTIVE);
        RegisterResponse representativeResponse = new RegisterResponse(representativeId, "apple_representative", "apple_representative@example.com", Role.COMPANY_REPRESENTATIVE, AccountStatus.ACTIVE);
        CompanyRepresentativeAssignmentResponse assignmentResponse = new CompanyRepresentativeAssignmentResponse(
                UUID.randomUUID(), companyId, representativeId, adminUserId,
                CompanyRepresentativeAssignmentRole.PRIMARY_CONTACT,
                CompanyRepresentativeAssignmentStatus.ACTIVE, null, null
        );

        when(companyService.createCompany(eq(companyRequest))).thenReturn(companyResponse);
        when(authService.registerCompanyRepresentative(eq(representativeRequest))).thenReturn(representativeResponse);
        when(companyRepresentativeAssignmentService.assignRepresentative(eq(companyId), eq(representativeId), eq(adminUserId))).thenReturn(assignmentResponse);

        CompanyOnboardingResponse response = companyOnboardingService.onboardCompany(request, adminUserId);

        assertThat(response.representative().id()).isEqualTo(representativeId);
        assertThat(response.assignment().assignedByUserId()).isEqualTo(adminUserId);
        assertThat(response.assignment().assignmentRole()).isEqualTo(CompanyRepresentativeAssignmentRole.PRIMARY_CONTACT);
    }
}