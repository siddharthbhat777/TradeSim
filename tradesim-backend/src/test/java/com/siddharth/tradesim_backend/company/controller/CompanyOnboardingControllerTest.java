package com.siddharth.tradesim_backend.company.controller;

import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.auth.model.dto.RegisterResponse;
import com.siddharth.tradesim_backend.company.enums.CompanyRepresentativeAssignmentRole;
import com.siddharth.tradesim_backend.company.enums.CompanyRepresentativeAssignmentStatus;
import com.siddharth.tradesim_backend.company.enums.CompanyStatus;
import com.siddharth.tradesim_backend.company.model.dto.CompanyOnboardingResponse;
import com.siddharth.tradesim_backend.company.model.dto.CompanyRepresentativeAssignmentResponse;
import com.siddharth.tradesim_backend.company.model.dto.CompanyResponse;
import com.siddharth.tradesim_backend.company.model.dto.CreateCompanyOnboardingRequest;
import com.siddharth.tradesim_backend.company.model.dto.CreateCompanyRequest;
import com.siddharth.tradesim_backend.company.service.CompanyOnboardingService;
import com.siddharth.tradesim_backend.company.service.CompanyRepresentativeAssignmentService;
import com.siddharth.tradesim_backend.company.service.CompanyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompanyOnboardingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompanyService companyService;

    @MockitoBean
    private CompanyRepresentativeAssignmentService companyRepresentativeAssignmentService;

    @MockitoBean
    private CompanyOnboardingService companyOnboardingService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminShouldOnboardCompany() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID representativeId = UUID.randomUUID();

        User admin = User.builder().id(adminId).username("admin").password("password").role(Role.ADMIN).accountStatus(AccountStatus.ACTIVE).build();
        UserPrincipal principal = new UserPrincipal(admin);

        CreateCompanyOnboardingRequest request = new CreateCompanyOnboardingRequest(
                new CreateCompanyRequest("Apple Inc", "APPLE", "United States"),
                new com.siddharth.tradesim_backend.auth.model.dto.RegisterRequest("apple_representative", "apple_representative@example.com", "Representative@123")
        );

        CompanyOnboardingResponse response = new CompanyOnboardingResponse(
                new CompanyResponse(companyId, "Apple Inc", "APPLE", "United States", CompanyStatus.ACTIVE),
                new RegisterResponse(representativeId, "apple_representative", "apple_representative@example.com", Role.COMPANY_REPRESENTATIVE, AccountStatus.ACTIVE),
                new CompanyRepresentativeAssignmentResponse(
                        UUID.randomUUID(), companyId, representativeId, adminId,
                        CompanyRepresentativeAssignmentRole.PRIMARY_CONTACT,
                        CompanyRepresentativeAssignmentStatus.ACTIVE, null, null
                )
        );

        when(companyOnboardingService.onboardCompany(eq(request), eq(adminId))).thenReturn(response);

        mockMvc.perform(post("/companies/onboard")
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.representative.id").value(representativeId.toString()))
                .andExpect(jsonPath("$.assignment.assignedByUserId").value(adminId.toString()))
                .andExpect(jsonPath("$.assignment.assignmentRole").value("PRIMARY_CONTACT"));
    }
}