package com.siddharth.tradesim_backend.exchange;

import com.siddharth.tradesim_backend.exchange.enums.ExchangeStatus;
import com.siddharth.tradesim_backend.exchange.model.dto.ExchangeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}