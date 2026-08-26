package com.siddharth.tradesim_backend.portfolio;

import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.auth.model.UserPrincipal;
import com.siddharth.tradesim_backend.portfolio.model.dto.PortfolioHoldingResponse;
import com.siddharth.tradesim_backend.portfolio.model.dto.PortfolioResponse;
import com.siddharth.tradesim_backend.portfolio.service.PortfolioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PortfolioService portfolioService;

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturnUserPortfolio() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .username("sid")
                .role(Role.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        UserPrincipal principal = new UserPrincipal(user);

        PortfolioHoldingResponse holding = createHolding(stockId);

        PortfolioResponse response = new PortfolioResponse(
                List.of(holding),
                new BigDecimal("500.00"),
                BigDecimal.ZERO,
                new BigDecimal("1500.00"),
                new BigDecimal("1200.00"),
                new BigDecimal("300.00"),
                BigDecimal.ZERO,
                new BigDecimal("300.00"),
                new BigDecimal("2000.00")
        );

        when(portfolioService.fetchPortfolio(userId)).thenReturn(response);

        mockMvc.perform(
                        get("/portfolio")
                                .with(authentication(
                                        new UsernamePasswordAuthenticationToken(
                                                principal,
                                                null,
                                                principal.getAuthorities()
                                        )
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holdings[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$.holdings[0].quantity").value(10))
                .andExpect(jsonPath("$.totalValue").value(1500.00));
    }

    private PortfolioHoldingResponse createHolding(UUID stockId) {
        return new PortfolioHoldingResponse(
                stockId,
                "AAPL",
                10,
                new BigDecimal("120.00"),
                new BigDecimal("150.00"),
                new BigDecimal("1500.00"),
                new BigDecimal("300.00")
        );
    }
}