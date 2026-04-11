package com.siddharth.tradesim_backend.ipo;

import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.ipo.enums.IpoOfferStatus;
import com.siddharth.tradesim_backend.ipo.enums.IpoSubscriptionStatus;
import com.siddharth.tradesim_backend.ipo.model.dto.CreateIpoOfferRequest;
import com.siddharth.tradesim_backend.ipo.model.dto.IpoOfferResponse;
import com.siddharth.tradesim_backend.ipo.model.dto.IpoSubscriptionResponse;
import com.siddharth.tradesim_backend.ipo.model.dto.RejectIpoOfferRequest;
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

import java.math.BigDecimal;
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
class IpoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IpoService ipoService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void companyRepresentativeShouldSubmitIpoOffer() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        UUID primaryContactUserId = UUID.randomUUID();
        UUID ipoOfferId = UUID.randomUUID();

        User representative = User.builder()
                .id(primaryContactUserId)
                .username("issuer_primary")
                .password("password")
                .role(Role.COMPANY_REPRESENTATIVE)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        UserPrincipal principal = new UserPrincipal(representative);

        CreateIpoOfferRequest request = new CreateIpoOfferRequest(
                BigDecimal.valueOf(125.50),
                100,
                5,
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(600)
        );

        IpoOfferResponse response = new IpoOfferResponse(
                ipoOfferId,
                companyId,
                stockId,
                primaryContactUserId,
                BigDecimal.valueOf(125.50),
                100,
                5,
                500,
                request.subscriptionStartAt(),
                request.subscriptionEndAt(),
                IpoOfferStatus.PENDING_APPROVAL,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        when(ipoService.submitIpoOffer(eq(companyId), eq(stockId), eq(primaryContactUserId), eq(request))).thenReturn(response);

        mockMvc.perform(post("/ipo-offers/{companyId}/stocks/{stockId}", companyId, stockId)
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.totalSharesOffered").value(500));
    }

    @Test
    void adminShouldFetchPendingIpoOffers() throws Exception {
        UUID adminId = UUID.randomUUID();

        User admin = User.builder()
                .id(adminId)
                .username("admin")
                .password("password")
                .role(Role.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        UserPrincipal principal = new UserPrincipal(admin);

        IpoOfferResponse response = new IpoOfferResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.valueOf(125.50),
                100,
                5,
                500,
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(600),
                IpoOfferStatus.PENDING_APPROVAL,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        when(ipoService.fetchPendingIpoOffers()).thenReturn(List.of(response));

        mockMvc.perform(get("/ipo-offers/pending")
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$[0].totalSharesOffered").value(500));
    }

    @Test
    void adminShouldApproveIpoOffer() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID ipoOfferId = UUID.randomUUID();

        User admin = User.builder()
                .id(adminId)
                .username("admin")
                .password("password")
                .role(Role.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        UserPrincipal principal = new UserPrincipal(admin);

        IpoOfferResponse response = new IpoOfferResponse(
                ipoOfferId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.valueOf(125.50),
                100,
                5,
                500,
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(600),
                IpoOfferStatus.SUBSCRIPTION_OPEN,
                adminId,
                Instant.now(),
                null,
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        when(ipoService.approveIpoOffer(eq(ipoOfferId), eq(adminId))).thenReturn(response);

        mockMvc.perform(put("/ipo-offers/{ipoOfferId}/approve", ipoOfferId)
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBSCRIPTION_OPEN"))
                .andExpect(jsonPath("$.reviewedByUserId").value(adminId.toString()));
    }

    @Test
    void userShouldSubscribeToOpenIpo() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID ipoOfferId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .username("ipo_user")
                .password("password")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        UserPrincipal principal = new UserPrincipal(user);

        IpoSubscriptionResponse response = new IpoSubscriptionResponse(
                UUID.randomUUID(),
                ipoOfferId,
                stockId,
                userId,
                BigDecimal.valueOf(125.50),
                BigDecimal.valueOf(12550),
                0,
                IpoSubscriptionStatus.SUBMITTED,
                Instant.now(),
                Instant.now()
        );

        when(ipoService.subscribeToIpo(eq(ipoOfferId), eq(userId))).thenReturn(response);

        mockMvc.perform(post("/ipo-offers/{ipoOfferId}/subscriptions", ipoOfferId)
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.lockedAmount").value(12550));
    }

    @Test
    void adminShouldFinalizeIpoOffer() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID ipoOfferId = UUID.randomUUID();

        User admin = User.builder()
                .id(adminId)
                .username("admin")
                .password("password")
                .role(Role.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        UserPrincipal principal = new UserPrincipal(admin);

        IpoOfferResponse response = new IpoOfferResponse(
                ipoOfferId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.valueOf(125.50),
                100,
                5,
                500,
                Instant.now().minusSeconds(600),
                Instant.now().minusSeconds(60),
                IpoOfferStatus.ALLOTTED,
                adminId,
                Instant.now(),
                adminId,
                Instant.now(),
                null,
                Instant.now(),
                Instant.now()
        );

        when(ipoService.finalizeIpoOffer(eq(ipoOfferId), eq(adminId))).thenReturn(response);

        mockMvc.perform(put("/ipo-offers/{ipoOfferId}/finalize", ipoOfferId)
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ALLOTTED"))
                .andExpect(jsonPath("$.finalizedByUserId").value(adminId.toString()));
    }

    @Test
    void plainUserShouldNotSubmitIpoOffer() throws Exception {
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

        CreateIpoOfferRequest request = new CreateIpoOfferRequest(
                BigDecimal.valueOf(125.50),
                100,
                5,
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(600)
        );

        mockMvc.perform(post("/ipo-offers/{companyId}/stocks/{stockId}", companyId, stockId)
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminShouldRejectIpoOffer() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID ipoOfferId = UUID.randomUUID();

        User admin = User.builder()
                .id(adminId)
                .username("admin")
                .password("password")
                .role(Role.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        UserPrincipal principal = new UserPrincipal(admin);

        RejectIpoOfferRequest request = new RejectIpoOfferRequest("IPO disclosures are incomplete");

        IpoOfferResponse response = new IpoOfferResponse(
                ipoOfferId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.valueOf(125.50),
                100,
                5,
                500,
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(600),
                IpoOfferStatus.REJECTED,
                adminId,
                Instant.now(),
                null,
                null,
                "IPO disclosures are incomplete",
                Instant.now(),
                Instant.now()
        );

        when(ipoService.rejectIpoOffer(eq(ipoOfferId), eq("IPO disclosures are incomplete"), eq(adminId))).thenReturn(response);

        mockMvc.perform(put("/ipo-offers/{ipoOfferId}/reject", ipoOfferId)
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("IPO disclosures are incomplete"));
    }
}