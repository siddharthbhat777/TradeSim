package com.siddharth.tradesim_backend.company.controller;

import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.company.enums.CompanyRepresentativeAssignmentStatus;
import com.siddharth.tradesim_backend.company.model.dto.AssignCompanyRepresentativeRequest;
import com.siddharth.tradesim_backend.company.model.dto.CompanyRepresentativeAssignmentResponse;
import com.siddharth.tradesim_backend.company.service.CompanyRepresentativeAssignmentService;
import com.siddharth.tradesim_backend.company.service.CompanyOnboardingService;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompanyRepresentativeAssignmentControllerTest {

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
    void adminShouldAssignCompanyRepresentative() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID representativeUserId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();

        User admin = User.builder()
                .id(adminId)
                .username("admin")
                .password("password")
                .role(Role.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        UserPrincipal principal = new UserPrincipal(admin);

        AssignCompanyRepresentativeRequest request = new AssignCompanyRepresentativeRequest(representativeUserId);

        CompanyRepresentativeAssignmentResponse response = new CompanyRepresentativeAssignmentResponse(
                assignmentId,
                companyId,
                representativeUserId,
                adminId,
                CompanyRepresentativeAssignmentStatus.ACTIVE,
                null
        );

        when(companyRepresentativeAssignmentService.assignRepresentative(eq(companyId), eq(representativeUserId), eq(adminId))).thenReturn(response);

        mockMvc.perform(
                        post("/companies/{companyId}/representatives", companyId)
                                .with(authentication(
                                        new UsernamePasswordAuthenticationToken(
                                                principal,
                                                null,
                                                principal.getAuthorities()
                                        )
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(assignmentId.toString()))
                .andExpect(jsonPath("$.companyId").value(companyId.toString()))
                .andExpect(jsonPath("$.userId").value(representativeUserId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void nonAdminShouldNotAssignCompanyRepresentative() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID representativeUserId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .username("normal_user")
                .password("password")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        UserPrincipal principal = new UserPrincipal(user);

        AssignCompanyRepresentativeRequest request = new AssignCompanyRepresentativeRequest(representativeUserId);

        mockMvc.perform(
                        post("/companies/{companyId}/representatives", companyId)
                                .with(authentication(
                                        new UsernamePasswordAuthenticationToken(
                                                principal,
                                                null,
                                                principal.getAuthorities()
                                        )
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void adminShouldFetchActiveCompanyRepresentatives() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        User admin = User.builder()
                .id(adminId)
                .username("admin")
                .password("password")
                .role(Role.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        UserPrincipal principal = new UserPrincipal(admin);

        CompanyRepresentativeAssignmentResponse response = new CompanyRepresentativeAssignmentResponse(
                UUID.randomUUID(),
                companyId,
                UUID.randomUUID(),
                adminId,
                CompanyRepresentativeAssignmentStatus.ACTIVE,
                null
        );

        when(companyRepresentativeAssignmentService.fetchActiveAssignments(eq(companyId))).thenReturn(List.of(response));

        mockMvc.perform(
                        get("/companies/{companyId}/representatives", companyId)
                                .with(authentication(
                                        new UsernamePasswordAuthenticationToken(
                                                principal,
                                                null,
                                                principal.getAuthorities()
                                        )
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].companyId").value(companyId.toString()))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void adminShouldRevokeCompanyRepresentative() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID representativeUserId = UUID.randomUUID();

        User admin = User.builder()
                .id(adminId)
                .username("admin")
                .password("password")
                .role(Role.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        UserPrincipal principal = new UserPrincipal(admin);

        CompanyRepresentativeAssignmentResponse response = new CompanyRepresentativeAssignmentResponse(
                UUID.randomUUID(),
                companyId,
                representativeUserId,
                adminId,
                CompanyRepresentativeAssignmentStatus.REVOKED,
                java.time.Instant.now()
        );

        when(companyRepresentativeAssignmentService.revokeRepresentative(eq(companyId), eq(representativeUserId))).thenReturn(response);

        mockMvc.perform(
                        delete("/companies/{companyId}/representatives/{userId}", companyId, representativeUserId)
                                .with(authentication(
                                        new UsernamePasswordAuthenticationToken(
                                                principal,
                                                null,
                                                principal.getAuthorities()
                                        )
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyId").value(companyId.toString()))
                .andExpect(jsonPath("$.userId").value(representativeUserId.toString()))
                .andExpect(jsonPath("$.status").value("REVOKED"));
    }
}