package com.siddharth.tradesim_backend.trade;

import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.trade.enums.OrderType;
import com.siddharth.tradesim_backend.trade.enums.Status;
import com.siddharth.tradesim_backend.trade.enums.Type;
import com.siddharth.tradesim_backend.trade.model.dto.TradeRequest;
import com.siddharth.tradesim_backend.trade.model.dto.TradeResponse;
import com.siddharth.tradesim_backend.trade.service.TradeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TradeService tradeService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldPlaceTradeOrder() throws Exception {
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .username("sid")
                .password("password")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .balance(BigDecimal.valueOf(10000))
                .build();

        UserPrincipal principal = new UserPrincipal(user);

        TradeRequest request = new TradeRequest();
        ReflectionTestUtils.setField(request, "stockId", UUID.randomUUID());
        ReflectionTestUtils.setField(request, "quantity", 10);
        ReflectionTestUtils.setField(request, "type", Type.BUY);
        ReflectionTestUtils.setField(request, "orderType", OrderType.MARKET);

        TradeResponse response = new TradeResponse(
                UUID.randomUUID(),
                request.getStockId(),
                "AAPL",
                Type.BUY,
                OrderType.MARKET,
                Status.EXECUTED,
                10,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(9000),
                Instant.now()
        );

        when(tradeService.placeOrder(eq(userId), any(TradeRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/trades/order")
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors
                                .authentication(
                                        new UsernamePasswordAuthenticationToken(
                                                principal,
                                                null,
                                                principal.getAuthorities()
                                        )
                                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andExpect(jsonPath("$.quantity").value(10));
    }

    @Test
    void shouldCancelTrade() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tradeId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .username("sid")
                .password("password")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .balance(BigDecimal.valueOf(10000))
                .build();

        UserPrincipal principal = new UserPrincipal(user);

        TradeResponse response = new TradeResponse(
                tradeId,
                UUID.randomUUID(),
                "AAPL",
                Type.BUY,
                OrderType.MARKET,
                Status.CANCELLED,
                10,
                null,
                null,
                BigDecimal.valueOf(10000),
                null
        );

        when(tradeService.cancelTrade(eq(tradeId), eq(userId)))
                .thenReturn(response);

        mockMvc.perform(
                        put("/trades/{tradeId}/cancel", tradeId)
                                .with(org.springframework.security.test.web.servlet.request
                                        .SecurityMockMvcRequestPostProcessors
                                        .authentication(
                                                new UsernamePasswordAuthenticationToken(
                                                        principal,
                                                        null,
                                                        principal.getAuthorities()
                                                )
                                        ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}