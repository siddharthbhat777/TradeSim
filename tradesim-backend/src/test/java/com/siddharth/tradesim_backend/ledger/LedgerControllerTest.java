package com.siddharth.tradesim_backend.ledger;

import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.ledger.enums.LedgerEntryType;
import com.siddharth.tradesim_backend.ledger.model.dto.LedgerEntryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LedgerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LedgerService ledgerService;

    @Test
    void authenticatedUserShouldFetchOwnLedger() throws Exception {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .username("sid")
                .password("password")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        UserPrincipal principal = new UserPrincipal(user);

        LedgerEntryResponse response = new LedgerEntryResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                userId,
                null,
                null,
                LedgerEntryType.INITIAL_CREDIT,
                BigDecimal.valueOf(10000000),
                BigDecimal.valueOf(10000000),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "Initial trading account funding",
                Instant.now()
        );

        when(ledgerService.fetchMyLedger(eq(userId))).thenReturn(List.of(response));

        mockMvc.perform(get("/ledger")
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("INITIAL_CREDIT"))
                .andExpect(jsonPath("$[0].amount").value(10000000));
    }

    @Test
    void unauthenticatedRequestShouldBeRejected() throws Exception {
        mockMvc.perform(get("/ledger")).andExpect(status().isUnauthorized());
    }
}