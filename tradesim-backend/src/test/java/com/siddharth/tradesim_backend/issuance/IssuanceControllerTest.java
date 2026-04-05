package com.siddharth.tradesim_backend.issuance;

import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.issuance.enums.IssuanceStatus;
import com.siddharth.tradesim_backend.issuance.model.dto.CreateIssuanceRequest;
import com.siddharth.tradesim_backend.issuance.model.dto.IssuanceRequestResponse;
import com.siddharth.tradesim_backend.issuance.model.dto.RejectIssuanceRequest;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IssuanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IssuanceService issuanceService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void companyRepresentativeShouldSubmitIssuanceRequest() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        UUID primaryContactUserId = UUID.randomUUID();
        UUID liquidityProviderUserId = UUID.randomUUID();
        UUID issuanceRequestId = UUID.randomUUID();

        User representative = User.builder()
                .id(primaryContactUserId)
                .username("issuer_primary")
                .password("password")
                .role(Role.COMPANY_REPRESENTATIVE)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        UserPrincipal principal = new UserPrincipal(representative);

        CreateIssuanceRequest request = new CreateIssuanceRequest(1_000_000, 200_000, liquidityProviderUserId);

        IssuanceRequestResponse response = new IssuanceRequestResponse(
                issuanceRequestId,
                companyId,
                stockId,
                primaryContactUserId,
                1_000_000,
                200_000,
                liquidityProviderUserId,
                IssuanceStatus.PENDING,
                null,
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        when(issuanceService.submitIssuanceRequest(eq(companyId), eq(stockId), eq(primaryContactUserId), eq(request))).thenReturn(response);

        mockMvc.perform(post("/issuance-requests/{companyId}/stocks/{stockId}", companyId, stockId)
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.tradableFloatShares").value(200000));
    }

    @Test
    void adminShouldFetchPendingIssuanceRequests() throws Exception {
        UUID adminId = UUID.randomUUID();

        User admin = User.builder()
                .id(adminId)
                .username("admin")
                .password("password")
                .role(Role.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        UserPrincipal principal = new UserPrincipal(admin);

        IssuanceRequestResponse response = new IssuanceRequestResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                1_000_000,
                200_000,
                UUID.randomUUID(),
                IssuanceStatus.PENDING,
                null,
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        when(issuanceService.fetchPendingIssuanceRequests()).thenReturn(List.of(response));

        mockMvc.perform(get("/issuance-requests/pending")
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].totalIssuedShares").value(1000000));
    }

    @Test
    void adminShouldApproveIssuanceRequest() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID issuanceRequestId = UUID.randomUUID();

        User admin = User.builder()
                .id(adminId)
                .username("admin")
                .password("password")
                .role(Role.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        UserPrincipal principal = new UserPrincipal(admin);

        IssuanceRequestResponse response = new IssuanceRequestResponse(
                issuanceRequestId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                1_000_000,
                200_000,
                UUID.randomUUID(),
                IssuanceStatus.APPROVED,
                adminId,
                Instant.now(),
                null,
                Instant.now(),
                Instant.now()
        );

        when(issuanceService.approveIssuanceRequest(eq(issuanceRequestId), eq(adminId))).thenReturn(response);

        mockMvc.perform(put("/issuance-requests/{issuanceRequestId}/approve", issuanceRequestId)
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewedByUserId").value(adminId.toString()));
    }

    @Test
    void adminShouldRejectIssuanceRequest() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID issuanceRequestId = UUID.randomUUID();

        User admin = User.builder()
                .id(adminId)
                .username("admin")
                .password("password")
                .role(Role.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        UserPrincipal principal = new UserPrincipal(admin);

        RejectIssuanceRequest request = new RejectIssuanceRequest("Issuer capitalization data is incomplete");

        IssuanceRequestResponse response = new IssuanceRequestResponse(
                issuanceRequestId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                1_000_000,
                200_000,
                UUID.randomUUID(),
                IssuanceStatus.REJECTED,
                adminId,
                Instant.now(),
                "Issuer capitalization data is incomplete",
                Instant.now(),
                Instant.now()
        );

        when(issuanceService.rejectIssuanceRequest(eq(issuanceRequestId), eq("Issuer capitalization data is incomplete"), eq(adminId))).thenReturn(response);

        mockMvc.perform(put("/issuance-requests/{issuanceRequestId}/reject", issuanceRequestId)
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Issuer capitalization data is incomplete"));
    }

    @Test
    void plainUserShouldNotSubmitIssuanceRequest() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .username("normal_user")
                .password("password")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        UserPrincipal principal = new UserPrincipal(user);

        CreateIssuanceRequest request = new CreateIssuanceRequest(1_000_000, 200_000, UUID.randomUUID());

        mockMvc.perform(post("/issuance-requests/{companyId}/stocks/{stockId}", companyId, stockId)
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}