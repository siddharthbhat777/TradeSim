package com.siddharth.tradesim_backend.stock;

import com.siddharth.tradesim_backend.stock.enums.MarketCapCategory;
import com.siddharth.tradesim_backend.stock.enums.Sector;
import com.siddharth.tradesim_backend.stock.enums.StockStatus;
import com.siddharth.tradesim_backend.stock.model.dto.StockResponse;
import com.siddharth.tradesim_backend.stock.service.StockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StockService stockService;

    @Test
    void shouldReturnSectors() throws Exception {
        when(stockService.fetchSectors()).thenReturn(List.of("TECHNOLOGY", "FINANCE"));

        mockMvc.perform(get("/stocks/sectors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("TECHNOLOGY"))
                .andExpect(jsonPath("$[1]").value("FINANCE"));
    }

    @Test
    void shouldReturnStocks() throws Exception {
        StockResponse stock = new StockResponse(
                UUID.randomUUID(),
                "AAPL",
                "Apple Inc",
                BigDecimal.valueOf(150.25),
                Sector.TECHNOLOGY,
                StockStatus.ACTIVE,
                0L,
                BigDecimal.ZERO,
                MarketCapCategory.UNKNOWN,
                "USD",
                UUID.randomUUID()
        );

        when(stockService.fetchStocks()).thenReturn(List.of(stock));

        mockMvc.perform(get("/stocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].companyName").value("Apple Inc"))
                .andExpect(jsonPath("$[0].currentPrice").value(150.25))
                .andExpect(jsonPath("$[0].status").value(StockStatus.ACTIVE.name()));
    }

    @Test
    void shouldReturnStockById() throws Exception {
        UUID stockId = UUID.randomUUID();
        StockResponse stock = new StockResponse(
                stockId,
                "AAPL",
                "Apple Inc",
                BigDecimal.valueOf(150.25),
                Sector.TECHNOLOGY,
                StockStatus.ACTIVE,
                0L,
                BigDecimal.ZERO,
                MarketCapCategory.UNKNOWN,
                "USD",
                UUID.randomUUID()
        );

        when(stockService.getStock(stockId)).thenReturn(stock);

        mockMvc.perform(get("/stocks/" + stockId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.companyName").value("Apple Inc"))
                .andExpect(jsonPath("$.currentPrice").value(150.25))
                .andExpect(jsonPath("$.status").value(StockStatus.ACTIVE.name()));
    }
}