package com.siddharth.tradesim_backend.company.controller;

import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.company.enums.CompanyStatus;
import com.siddharth.tradesim_backend.company.model.dto.CompanyResponse;
import com.siddharth.tradesim_backend.company.service.CompanyRepresentativeAssignmentService;
import com.siddharth.tradesim_backend.company.service.CompanyOnboardingService;
import com.siddharth.tradesim_backend.company.service.CompanyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompanyService companyService;

    @MockitoBean
    private CompanyRepresentativeAssignmentService companyRepresentativeAssignmentService;

    @MockitoBean
    private CompanyOnboardingService companyOnboardingService;

    @Test
    void shouldReturnCompanies() throws Exception {
        UUID adminId = UUID.randomUUID();
        User admin = User.builder().id(adminId).username("admin").password("password").role(Role.ADMIN).accountStatus(AccountStatus.ACTIVE).build();
        UserPrincipal principal = new UserPrincipal(admin);

        UUID primaryContactId = UUID.randomUUID();
        CompanyResponse company = new CompanyResponse(
                UUID.randomUUID(),
                "Apple Inc",
                "APPLE",
                "United States",
                CompanyStatus.ACTIVE,
                primaryContactId
        );

        when(companyService.fetchCompanies()).thenReturn(List.of(company));

        mockMvc.perform(get("/companies")
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Apple Inc"))
                .andExpect(jsonPath("$[0].code").value("APPLE"))
                .andExpect(jsonPath("$[0].country").value("United States"))
                .andExpect(jsonPath("$[0].status").value(CompanyStatus.ACTIVE.name()))
                .andExpect(jsonPath("$[0].primaryContactId").value(primaryContactId.toString()));
    }

    @Test
    void shouldReturnAssignedCompanies() throws Exception {
        UUID repId = UUID.randomUUID();
        User rep = User.builder().id(repId).username("rep").password("password").role(Role.COMPANY_REPRESENTATIVE).accountStatus(AccountStatus.ACTIVE).build();
        UserPrincipal principal = new UserPrincipal(rep);

        UUID primaryContactId = UUID.randomUUID();
        CompanyResponse company = new CompanyResponse(
                UUID.randomUUID(),
                "Apple Inc",
                "APPLE",
                "United States",
                CompanyStatus.ACTIVE,
                primaryContactId
        );

        when(companyService.fetchAssignedCompanies(repId)).thenReturn(List.of(company));

        mockMvc.perform(get("/companies/assigned")
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Apple Inc"))
                .andExpect(jsonPath("$[0].code").value("APPLE"))
                .andExpect(jsonPath("$[0].country").value("United States"))
                .andExpect(jsonPath("$[0].status").value(CompanyStatus.ACTIVE.name()))
                .andExpect(jsonPath("$[0].primaryContactId").value(primaryContactId.toString()));
    }

    @Test
    void shouldReturnCompanyById() throws Exception {
        UUID adminId = UUID.randomUUID();
        User admin = User.builder().id(adminId).username("admin").password("password").role(Role.ADMIN).accountStatus(AccountStatus.ACTIVE).build();
        UserPrincipal principal = new UserPrincipal(admin);

        UUID companyId = UUID.randomUUID();
        CompanyResponse company = new CompanyResponse(
                companyId,
                "Apple Inc",
                "APPLE",
                "United States",
                CompanyStatus.ACTIVE,
                null
        );

        when(companyService.fetchCompany(companyId)).thenReturn(company);

        mockMvc.perform(get("/companies/{companyId}", companyId)
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(companyId.toString()))
                .andExpect(jsonPath("$.code").value("APPLE"))
                .andExpect(jsonPath("$.country").value("United States"));
    }
}