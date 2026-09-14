package com.siddharth.tradesim_backend.trading_account;

import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.trading_account.model.dto.TradingAccountResponse;
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
class TradingAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TradingAccountService tradingAccountService;

    @Test
    void authenticatedUserShouldFetchOwnTradingAccount() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tradingAccountId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .username("sid")
                .password("password")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        UserPrincipal principal = new UserPrincipal(user);

        TradingAccountResponse response = new TradingAccountResponse(
                tradingAccountId,
                userId,
                "INR",
                BigDecimal.ZERO,
                5,
                BigDecimal.valueOf(25),
                Instant.now(),
                Instant.now()
        );

        when(tradingAccountService.fetchMyTradingAccount(eq(userId))).thenReturn(response);

        mockMvc.perform(get("/trading-account")
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tradingAccountId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.baseCurrency").value("INR"))
                .andExpect(jsonPath("$.marginLoan").value(0));
    }

    @Test
    void unauthenticatedRequestShouldBeRejected() throws Exception {
        mockMvc.perform(get("/trading-account"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication required."));
    }

    @Test
    void invalidTokenShouldReturnUnauthorizedJson() throws Exception {
        mockMvc.perform(get("/trading-account")
                        .header("Authorization", "Bearer invalid-accessToken"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication required."));
    }
}