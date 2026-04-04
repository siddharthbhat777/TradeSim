package com.siddharth.tradesim_backend.listing;

import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.listing.enums.ListingStatus;
import com.siddharth.tradesim_backend.listing.model.dto.CreateListingRequest;
import com.siddharth.tradesim_backend.listing.model.dto.ListingRequestResponse;
import com.siddharth.tradesim_backend.listing.model.dto.RejectListingRequest;
import com.siddharth.tradesim_backend.stock.enums.Sector;
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
class ListingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListingService listingService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void companyRepresentativeShouldSubmitListingRequest() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID representativeUserId = UUID.randomUUID();
        UUID exchangeId = UUID.randomUUID();
        UUID listingRequestId = UUID.randomUUID();

        User representative = User.builder()
                .id(representativeUserId)
                .username("issuer_rep")
                .password("password")
                .role(Role.COMPANY_REPRESENTATIVE)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        UserPrincipal principal = new UserPrincipal(representative);

        CreateListingRequest request = new CreateListingRequest(
                "INFY",
                exchangeId,
                BigDecimal.valueOf(1500.25),
                Sector.TECHNOLOGY,
                BigDecimal.TEN
        );

        ListingRequestResponse response = new ListingRequestResponse(
                listingRequestId,
                companyId,
                representativeUserId,
                "INFY",
                exchangeId,
                BigDecimal.valueOf(1500.25),
                Sector.TECHNOLOGY,
                BigDecimal.TEN,
                ListingStatus.PENDING,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        when(listingService.submitListingRequest(eq(companyId), eq(representativeUserId), eq(request))).thenReturn(response);

        mockMvc.perform(post("/companies/{companyId}/listing-requests", companyId)
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symbol").value("INFY"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void adminShouldFetchPendingListingRequests() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID exchangeId = UUID.randomUUID();

        User admin = User.builder()
                .id(adminId)
                .username("admin")
                .password("password")
                .role(Role.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        UserPrincipal principal = new UserPrincipal(admin);

        ListingRequestResponse response = new ListingRequestResponse(
                UUID.randomUUID(),
                companyId,
                UUID.randomUUID(),
                "INFY",
                exchangeId,
                BigDecimal.valueOf(1500.25),
                Sector.TECHNOLOGY,
                BigDecimal.TEN,
                ListingStatus.PENDING,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        when(listingService.fetchPendingListingRequests()).thenReturn(List.of(response));

        mockMvc.perform(get("/listing-requests/pending").with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("INFY"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void adminShouldApproveListingRequest() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID listingRequestId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        User admin = User.builder()
                .id(adminId)
                .username("admin")
                .password("password")
                .role(Role.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        UserPrincipal principal = new UserPrincipal(admin);

        ListingRequestResponse response = new ListingRequestResponse(
                listingRequestId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "INFY",
                UUID.randomUUID(),
                BigDecimal.valueOf(1500.25),
                Sector.TECHNOLOGY,
                BigDecimal.TEN,
                ListingStatus.APPROVED,
                adminId,
                Instant.now(),
                stockId,
                null,
                Instant.now(),
                Instant.now()
        );

        when(listingService.approveListingRequest(eq(listingRequestId), eq(adminId))).thenReturn(response);

        mockMvc.perform(put("/listing-requests/{listingRequestId}/approve", listingRequestId).with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvedStockId").value(stockId.toString()));
    }

    @Test
    void adminShouldRejectListingRequest() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID listingRequestId = UUID.randomUUID();

        User admin = User.builder()
                .id(adminId)
                .username("admin")
                .password("password")
                .role(Role.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        UserPrincipal principal = new UserPrincipal(admin);

        RejectListingRequest request = new RejectListingRequest("Incomplete issuer details");

        ListingRequestResponse response = new ListingRequestResponse(
                listingRequestId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "INFY",
                UUID.randomUUID(),
                BigDecimal.valueOf(1500.25),
                Sector.TECHNOLOGY,
                BigDecimal.TEN,
                ListingStatus.REJECTED,
                adminId,
                Instant.now(),
                null,
                "Incomplete issuer details",
                Instant.now(),
                Instant.now()
        );

        when(listingService.rejectListingRequest(eq(listingRequestId), eq("Incomplete issuer details"), eq(adminId))).thenReturn(response);

        mockMvc.perform(put("/listing-requests/{listingRequestId}/reject", listingRequestId)
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Incomplete issuer details"));
    }

    @Test
    void plainUserShouldNotSubmitListingRequest() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID exchangeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .username("normal_user")
                .password("password")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        UserPrincipal principal = new UserPrincipal(user);

        CreateListingRequest request = new CreateListingRequest(
                "INFY",
                exchangeId,
                BigDecimal.valueOf(1500.25),
                Sector.TECHNOLOGY,
                BigDecimal.TEN
        );

        mockMvc.perform(post("/companies/{companyId}/listing-requests", companyId)
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}