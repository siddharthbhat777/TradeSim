package com.siddharth.tradesim_backend.wallet;

import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.wallet.enums.MultiCurrencyStatus;
import com.siddharth.tradesim_backend.wallet.model.dto.CurrencyConversionRequest;
import com.siddharth.tradesim_backend.wallet.model.dto.WalletBucketResponse;
import com.siddharth.tradesim_backend.wallet.model.dto.WalletResponse;
import com.siddharth.tradesim_backend.wallet.model.dto.WalletTransactionRequest;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
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
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WalletService walletService;

    @Test
    void shouldFetchMyWallet() throws Exception {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(userId, Role.USER);

        WalletResponse response = new WalletResponse(
                UUID.randomUUID(),
                userId,
                MultiCurrencyStatus.APPROVED,
                List.of(new WalletBucketResponse(UUID.randomUUID(), "INR", BigDecimal.valueOf(1000), BigDecimal.ZERO, BigDecimal.valueOf(1000)))
        );

        when(walletService.fetchMyWallet(userId)).thenReturn(response);

        mockMvc.perform(get("/wallet")
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.multiCurrencyStatus").value("APPROVED"))
                .andExpect(jsonPath("$.buckets[0].currency").value("INR"));
    }

    @Test
    void shouldDepositFromBank() throws Exception {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(userId, Role.USER);
        WalletTransactionRequest request = new WalletTransactionRequest(BigDecimal.valueOf(500));

        WalletResponse response = new WalletResponse(UUID.randomUUID(), userId, MultiCurrencyStatus.UNREQUESTED, List.of());

        when(walletService.depositFromBank(eq(userId), any(BigDecimal.class))).thenReturn(response);

        mockMvc.perform(post("/wallet/deposit")
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRequestMultiCurrencyAccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(userId, Role.USER);

        WalletResponse response = new WalletResponse(UUID.randomUUID(), userId, MultiCurrencyStatus.PENDING, List.of());

        when(walletService.requestMultiCurrencyAccess(userId)).thenReturn(response);

        mockMvc.perform(post("/wallet/multi-currency/request")
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.multiCurrencyStatus").value("PENDING"));
    }

    @Test
    void shouldConvertCurrency() throws Exception {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(userId, Role.USER);
        CurrencyConversionRequest request = new CurrencyConversionRequest("INR", "USD", BigDecimal.valueOf(1000));

        WalletResponse response = new WalletResponse(UUID.randomUUID(), userId, MultiCurrencyStatus.APPROVED, List.of());

        when(walletService.convertCurrency(eq(userId), any(CurrencyConversionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/wallet/convert")
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void adminShouldApproveMultiCurrency() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        UserPrincipal principal = createPrincipal(adminId, Role.ADMIN);

        WalletResponse response = new WalletResponse(walletId, UUID.randomUUID(), MultiCurrencyStatus.APPROVED, List.of());

        when(walletService.approveMultiCurrencyAccess(walletId)).thenReturn(response);

        mockMvc.perform(put("/wallet/multi-currency/{walletId}/approve", walletId)
                        .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.multiCurrencyStatus").value("APPROVED"));
    }

    private UserPrincipal createPrincipal(UUID userId, Role role) {
        User user = User.builder().id(userId).role(role).accountStatus(AccountStatus.ACTIVE).build();
        return new UserPrincipal(user);
    }
}