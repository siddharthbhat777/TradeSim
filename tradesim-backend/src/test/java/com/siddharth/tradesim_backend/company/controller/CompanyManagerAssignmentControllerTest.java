package com.siddharth.tradesim_backend.company.controller;

import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.company.enums.CompanyManagerAssignmentStatus;
import com.siddharth.tradesim_backend.company.model.dto.AssignCompanyManagerRequest;
import com.siddharth.tradesim_backend.company.model.dto.CompanyManagerAssignmentResponse;
import com.siddharth.tradesim_backend.company.service.CompanyManagerAssignmentService;
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
class CompanyManagerAssignmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompanyService companyService;

    @MockitoBean
    private CompanyManagerAssignmentService companyManagerAssignmentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminShouldAssignCompanyManager() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID managerUserId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();

        User admin = User.builder()
                .id(adminId)
                .username("admin")
                .password("password")
                .role(Role.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        UserPrincipal principal = new UserPrincipal(admin);

        AssignCompanyManagerRequest request = new AssignCompanyManagerRequest(managerUserId);

        CompanyManagerAssignmentResponse response = new CompanyManagerAssignmentResponse(
                assignmentId,
                companyId,
                managerUserId,
                adminId,
                CompanyManagerAssignmentStatus.ACTIVE,
                null
        );

        when(companyManagerAssignmentService.assignManager(eq(companyId), eq(managerUserId), eq(adminId))).thenReturn(response);

        mockMvc.perform(
                        post("/companies/{companyId}/managers", companyId)
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
                .andExpect(jsonPath("$.userId").value(managerUserId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void nonAdminShouldNotAssignCompanyManager() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID managerUserId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .username("normal_user")
                .password("password")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        UserPrincipal principal = new UserPrincipal(user);

        AssignCompanyManagerRequest request = new AssignCompanyManagerRequest(managerUserId);

        mockMvc.perform(
                        post("/companies/{companyId}/managers", companyId)
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
    void adminShouldFetchActiveCompanyManagers() throws Exception {
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

        CompanyManagerAssignmentResponse response = new CompanyManagerAssignmentResponse(
                UUID.randomUUID(),
                companyId,
                UUID.randomUUID(),
                adminId,
                CompanyManagerAssignmentStatus.ACTIVE,
                null
        );

        when(companyManagerAssignmentService.fetchActiveAssignments(eq(companyId)))
                .thenReturn(List.of(response));

        mockMvc.perform(
                        get("/companies/{companyId}/managers", companyId)
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
    void adminShouldRevokeCompanyManager() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID managerUserId = UUID.randomUUID();

        User admin = User.builder()
                .id(adminId)
                .username("admin")
                .password("password")
                .role(Role.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        UserPrincipal principal = new UserPrincipal(admin);

        CompanyManagerAssignmentResponse response = new CompanyManagerAssignmentResponse(
                UUID.randomUUID(),
                companyId,
                managerUserId,
                adminId,
                CompanyManagerAssignmentStatus.REVOKED,
                java.time.Instant.now()
        );

        when(companyManagerAssignmentService.revokeManager(eq(companyId), eq(managerUserId))).thenReturn(response);

        mockMvc.perform(
                        delete("/companies/{companyId}/managers/{userId}", companyId, managerUserId)
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
                .andExpect(jsonPath("$.userId").value(managerUserId.toString()))
                .andExpect(jsonPath("$.status").value("REVOKED"));
    }
}