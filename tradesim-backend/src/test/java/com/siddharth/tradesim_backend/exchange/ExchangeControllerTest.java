package com.siddharth.tradesim_backend.exchange;

import com.siddharth.tradesim_backend.exchange.enums.ExchangeStatus;
import com.siddharth.tradesim_backend.exchange.model.dto.ExchangeMarketClockResponse;
import com.siddharth.tradesim_backend.exchange.model.dto.ExchangeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ExchangeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExchangeService exchangeService;

    @Test
    void shouldReturnExchanges() throws Exception {
        ExchangeResponse exchange = new ExchangeResponse(
                UUID.randomUUID(),
                "TradeSim National Exchange",
                "TSX",
                "India",
                "Asia/Kolkata",
                "INR",
                LocalTime.of(9, 15),
                LocalTime.of(15, 30),
                ExchangeStatus.ACTIVE
        );

        when(exchangeService.fetchExchanges()).thenReturn(List.of(exchange));

        mockMvc.perform(get("/exchanges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("TradeSim National Exchange"))
                .andExpect(jsonPath("$[0].code").value("TSX"))
                .andExpect(jsonPath("$[0].currency").value("INR"))
                .andExpect(jsonPath("$[0].status").value(ExchangeStatus.ACTIVE.name()));
    }

    @Test
    void shouldReturnExchangeById() throws Exception {
        UUID exchangeId = UUID.randomUUID();
        ExchangeResponse exchange = new ExchangeResponse(
                exchangeId,
                "TradeSim National Exchange",
                "TSX",
                "India",
                "Asia/Kolkata",
                "INR",
                LocalTime.of(9, 15),
                LocalTime.of(15, 30),
                ExchangeStatus.ACTIVE
        );

        when(exchangeService.fetchExchange(exchangeId)).thenReturn(exchange);

        mockMvc.perform(get("/exchanges/{exchangeId}", exchangeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(exchangeId.toString()))
                .andExpect(jsonPath("$.code").value("TSX"))
                .andExpect(jsonPath("$.country").value("India"));
    }

    @Test
    void shouldReturnMarketClock() throws Exception {
        UUID exchangeId = UUID.randomUUID();
        ExchangeMarketClockResponse response = new ExchangeMarketClockResponse(
                exchangeId,
                "NYSE",
                "NYSE Demo",
                "America/New_York",
                LocalDate.of(2026, 4, 13),
                LocalTime.of(10, 0),
                DayOfWeek.MONDAY,
                LocalTime.of(9, 30),
                LocalTime.of(16, 0),
                true,
                true,
                Instant.parse("2026-04-13T14:00:00Z"),
                Instant.parse("2026-04-13T13:30:00Z"),
                Instant.parse("2026-04-13T20:00:00Z")
        );

        when(exchangeService.fetchMarketClock(exchangeId)).thenReturn(response);

        mockMvc.perform(get("/exchanges/{exchangeId}/market-clock", exchangeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exchangeId").value(exchangeId.toString()))
                .andExpect(jsonPath("$.timezone").value("America/New_York"))
                .andExpect(jsonPath("$.localDate").value("2026-04-13"))
                .andExpect(jsonPath("$.localTime").value("10:00:00"))
                .andExpect(jsonPath("$.marketOpenNow").value(true));
    }

    @Test
    void invalidExchangeIdShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/exchanges/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_PATH_VARIABLE"))
                .andExpect(jsonPath("$.fieldErrors.exchangeId").value("Expected UUID format."));
    }

    @Test
    void missingExchangeShouldReturnNotFound() throws Exception {
        UUID exchangeId = UUID.randomUUID();
        when(exchangeService.fetchExchange(exchangeId)).thenThrow(ExchangeException.notFound("Exchange not found"));

        mockMvc.perform(get("/exchanges/{exchangeId}", exchangeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("EXCHANGE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Exchange not found"));
    }

    @Test
    void wrongMethodShouldReturnMethodNotAllowed() throws Exception {
        mockMvc.perform(post("/exchanges/{exchangeId}", UUID.randomUUID()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));
    }
}