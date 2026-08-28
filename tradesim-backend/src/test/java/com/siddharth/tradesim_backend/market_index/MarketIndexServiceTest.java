package com.siddharth.tradesim_backend.market_index;

import com.siddharth.tradesim_backend.exchange.ExchangeRepository;
import com.siddharth.tradesim_backend.exchange.ExchangeService;
import com.siddharth.tradesim_backend.market_index.model.MarketIndex;
import com.siddharth.tradesim_backend.market_index.model.MarketIndexConstituent;
import com.siddharth.tradesim_backend.market_index.model.dto.AddConstituentRequest;
import com.siddharth.tradesim_backend.market_index.model.dto.CreateMarketIndexRequest;
import com.siddharth.tradesim_backend.market_index.model.dto.MarketIndexConstituentResponse;
import com.siddharth.tradesim_backend.market_index.model.dto.MarketIndexResponse;
import com.siddharth.tradesim_backend.market_index.repository.MarketIndexConstituentRepository;
import com.siddharth.tradesim_backend.market_index.repository.MarketIndexRepository;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.model.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketIndexServiceTest {

    @Mock
    private MarketIndexRepository marketIndexRepository;

    @Mock
    private MarketIndexConstituentRepository marketIndexConstituentRepository;

    @Mock
    private ExchangeRepository exchangeRepository;

    @Mock
    private ExchangeService exchangeService;

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private MarketIndexService marketIndexService;

    private UUID indexId;
    private UUID exchangeId;
    private UUID stockId;
    private MarketIndex marketIndex;
    private Stock stock;

    @BeforeEach
    void setUp() {
        indexId = UUID.randomUUID();
        exchangeId = UUID.randomUUID();
        stockId = UUID.randomUUID();

        marketIndex = MarketIndex.builder()
                .id(indexId)
                .name("TradeSim Tech 50")
                .symbol("TECH50")
                .exchangeId(exchangeId)
                .baseValue(BigDecimal.valueOf(1000))
                .build();

        stock = Stock.builder()
                .id(stockId)
                .symbol("AAPL")
                .companyName("Apple Inc")
                .exchangeId(exchangeId)
                .lastTradedPrice(BigDecimal.valueOf(150))
                .tradableFloatShares(1000000)
                .build();
    }

    @Test
    void shouldFetchAllIndices() {
        when(marketIndexRepository.findAll()).thenReturn(List.of(marketIndex));

        List<MarketIndexResponse> responses = marketIndexService.fetchAllIndices();

        assertFalse(responses.isEmpty());
        assertEquals("TECH50", responses.getFirst().symbol());
        verify(marketIndexRepository).findAll();
    }

    @Test
    void shouldFetchIndicesByExchange() {
        when(marketIndexRepository.findByExchangeId(exchangeId)).thenReturn(List.of(marketIndex));

        List<MarketIndexResponse> responses = marketIndexService.fetchIndicesByExchange(exchangeId);

        assertFalse(responses.isEmpty());
        assertEquals(exchangeId, responses.getFirst().exchangeId());
        verify(marketIndexRepository).findByExchangeId(exchangeId);
    }

    @Test
    void shouldFetchConstituents() {
        MarketIndexConstituent constituent = new MarketIndexConstituent(UUID.randomUUID(), indexId, stockId);
        when(marketIndexRepository.existsById(indexId)).thenReturn(true);
        when(marketIndexConstituentRepository.findByIndexId(indexId)).thenReturn(List.of(constituent));
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));

        List<MarketIndexConstituentResponse> responses = marketIndexService.fetchConstituents(indexId);

        assertFalse(responses.isEmpty());
        assertEquals(stockId, responses.getFirst().stockId());
        assertEquals("AAPL", responses.getFirst().symbol());
    }

    @Test
    void shouldCreateIndexSuccessfully() {
        CreateMarketIndexRequest request = new CreateMarketIndexRequest("TradeSim Tech 50", "TECH50", exchangeId, BigDecimal.valueOf(1000));
        when(marketIndexRepository.existsBySymbol("TECH50")).thenReturn(false);
        when(exchangeRepository.existsById(exchangeId)).thenReturn(true);
        when(marketIndexRepository.save(any(MarketIndex.class))).thenReturn(marketIndex);

        MarketIndexResponse response = marketIndexService.createIndex(request);

        assertNotNull(response);
        assertEquals("TECH50", response.symbol());
        verify(marketIndexRepository).save(any(MarketIndex.class));
    }

    @Test
    void shouldThrowExceptionWhenIndexSymbolExists() {
        CreateMarketIndexRequest request = new CreateMarketIndexRequest("TradeSim Tech 50", "TECH50", exchangeId, BigDecimal.valueOf(1000));
        when(marketIndexRepository.existsBySymbol("TECH50")).thenReturn(true);

        assertThrows(MarketIndexException.class, () -> marketIndexService.createIndex(request));
        verify(marketIndexRepository, never()).save(any());
    }

    @Test
    void shouldAddConstituentSuccessfully() {
        AddConstituentRequest request = new AddConstituentRequest(stockId);
        when(marketIndexRepository.findById(indexId)).thenReturn(Optional.of(marketIndex));
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(marketIndexConstituentRepository.existsByIndexIdAndStockId(indexId, stockId)).thenReturn(false);

        marketIndexService.addConstituent(indexId, request);

        verify(marketIndexConstituentRepository).save(any(MarketIndexConstituent.class));
    }

    @Test
    void shouldThrowExceptionWhenExchangeMismatchOnAddConstituent() {
        stock.setExchangeId(UUID.randomUUID());
        AddConstituentRequest request = new AddConstituentRequest(stockId);
        when(marketIndexRepository.findById(indexId)).thenReturn(Optional.of(marketIndex));
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));

        assertThrows(MarketIndexException.class, () -> marketIndexService.addConstituent(indexId, request));
        verify(marketIndexConstituentRepository, never()).save(any());
    }

    @Test
    void shouldRemoveConstituentSuccessfully() {
        MarketIndexConstituent constituent = new MarketIndexConstituent(UUID.randomUUID(), indexId, stockId);
        when(marketIndexConstituentRepository.findByIndexId(indexId)).thenReturn(List.of(constituent));

        marketIndexService.removeConstituent(indexId, stockId);

        verify(marketIndexConstituentRepository).delete(constituent);
    }

    @Test
    void shouldInitializeIndexSuccessfully() {
        MarketIndexConstituent constituent = new MarketIndexConstituent(UUID.randomUUID(), indexId, stockId);
        when(marketIndexRepository.findById(indexId)).thenReturn(Optional.of(marketIndex));
        when(marketIndexConstituentRepository.findByIndexId(indexId)).thenReturn(List.of(constituent));
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(exchangeService.currentExchangeDate(exchangeId)).thenReturn(LocalDate.now());
        when(marketIndexRepository.save(any(MarketIndex.class))).thenReturn(marketIndex);

        MarketIndexResponse response = marketIndexService.initializeIndex(indexId);

        assertNotNull(response);
        verify(marketIndexRepository).save(marketIndex);
        assertNotNull(marketIndex.getBaseMarketCap());
    }

    @Test
    void shouldUpdateIndicesForStock() {
        MarketIndexConstituent constituent = new MarketIndexConstituent(UUID.randomUUID(), indexId, stockId);
        marketIndex.setBaseMarketCap(BigDecimal.valueOf(150000000));
        when(marketIndexConstituentRepository.findByStockId(stockId)).thenReturn(List.of(constituent));
        when(marketIndexRepository.findById(indexId)).thenReturn(Optional.of(marketIndex));
        when(marketIndexConstituentRepository.findByIndexId(indexId)).thenReturn(List.of(constituent));
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(exchangeService.currentExchangeDate(exchangeId)).thenReturn(LocalDate.now());

        marketIndexService.updateIndicesForStock(stockId);

        verify(marketIndexRepository).save(marketIndex);
    }
}