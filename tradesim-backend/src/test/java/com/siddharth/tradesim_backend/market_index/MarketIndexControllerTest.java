package com.siddharth.tradesim_backend.market_index;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siddharth.tradesim_backend.market_index.model.dto.AddConstituentRequest;
import com.siddharth.tradesim_backend.market_index.model.dto.CreateMarketIndexRequest;
import com.siddharth.tradesim_backend.market_index.model.dto.MarketIndexConstituentResponse;
import com.siddharth.tradesim_backend.market_index.model.dto.MarketIndexResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MarketIndexControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MarketIndexService marketIndexService;

    @InjectMocks
    private MarketIndexController marketIndexController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID indexId;
    private UUID exchangeId;
    private UUID stockId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(marketIndexController).build();
        indexId = UUID.randomUUID();
        exchangeId = UUID.randomUUID();
        stockId = UUID.randomUUID();
    }

    @Test
    void shouldGetAllIndices() throws Exception {
        MarketIndexResponse response = new MarketIndexResponse(indexId, "Test Index", "TST", exchangeId, BigDecimal.valueOf(100), null, null, null, null, null, null, null);
        when(marketIndexService.fetchAllIndices()).thenReturn(List.of(response));

        mockMvc.perform(get("/indices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("TST"));

        verify(marketIndexService).fetchAllIndices();
    }

    @Test
    void shouldGetIndicesByExchange() throws Exception {
        MarketIndexResponse response = new MarketIndexResponse(indexId, "Test Index", "TST", exchangeId, BigDecimal.valueOf(100), null, null, null, null, null, null, null);
        when(marketIndexService.fetchIndicesByExchange(exchangeId)).thenReturn(List.of(response));

        mockMvc.perform(get("/indices/exchange/{exchangeId}", exchangeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("TST"));

        verify(marketIndexService).fetchIndicesByExchange(exchangeId);
    }

    @Test
    void shouldGetConstituents() throws Exception {
        MarketIndexConstituentResponse constituentResponse = new MarketIndexConstituentResponse(stockId, "AAPL", "Apple Inc");
        when(marketIndexService.fetchConstituents(indexId)).thenReturn(List.of(constituentResponse));

        mockMvc.perform(get("/indices/{indexId}/constituents", indexId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"));

        verify(marketIndexService).fetchConstituents(indexId);
    }

    @Test
    void shouldCreateIndex() throws Exception {
        CreateMarketIndexRequest request = new CreateMarketIndexRequest("Test Index", "TST", exchangeId, BigDecimal.valueOf(100));
        MarketIndexResponse response = new MarketIndexResponse(indexId, "Test Index", "TST", exchangeId, BigDecimal.valueOf(100), null, null, null, null, null, null, null);
        when(marketIndexService.createIndex(any(CreateMarketIndexRequest.class))).thenReturn(response);

        mockMvc.perform(post("/indices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symbol").value("TST"));

        verify(marketIndexService).createIndex(any(CreateMarketIndexRequest.class));
    }

    @Test
    void shouldInitializeIndex() throws Exception {
        MarketIndexResponse response = new MarketIndexResponse(indexId, "Test Index", "TST", exchangeId, BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.ZERO, BigDecimal.ZERO, null, null, null, null);
        when(marketIndexService.initializeIndex(indexId)).thenReturn(response);

        mockMvc.perform(post("/indices/{indexId}/initialize", indexId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentValue").value(100));

        verify(marketIndexService).initializeIndex(indexId);
    }

    @Test
    void shouldAddConstituent() throws Exception {
        AddConstituentRequest request = new AddConstituentRequest(stockId);

        mockMvc.perform(post("/indices/{indexId}/constituents", indexId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(marketIndexService).addConstituent(eq(indexId), any(AddConstituentRequest.class));
    }

    @Test
    void shouldRemoveConstituent() throws Exception {
        mockMvc.perform(delete("/indices/{indexId}/constituents/{stockId}", indexId, stockId))
                .andExpect(status().isNoContent());

        verify(marketIndexService).removeConstituent(indexId, stockId);
    }
}