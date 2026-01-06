package com.siddharth.tradesim_backend.stock.service;

import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.enums.StockStatus;
import com.siddharth.tradesim_backend.stock.model.Stock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockPriceFluctuationServiceTest {

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private StockPriceFluctuationService fluctuationService;

    @Test
    void shouldFluctuatePriceForActiveStock() {
        Stock stock = Stock.builder()
                .id(UUID.randomUUID())
                .symbol("AAPL")
                .currentPrice(new BigDecimal("150.0000"))
                .status(StockStatus.ACTIVE)
                .build();

        when(stockRepository.findByStatus(StockStatus.ACTIVE)).thenReturn(List.of(stock));

        fluctuationService.fluctuatePrices();

        verify(stockRepository).save(stock);
        assertThat(stock.getCurrentPrice()).isNotEqualByComparingTo("150.0000");
    }

    @Test
    void shouldNeverDropPriceBelowOne() {
        Stock stock = Stock.builder()
                .id(UUID.randomUUID())
                .symbol("LOW")
                .currentPrice(BigDecimal.ONE)
                .status(StockStatus.ACTIVE)
                .build();

        when(stockRepository.findByStatus(StockStatus.ACTIVE)).thenReturn(List.of(stock));

        fluctuationService.fluctuatePrices();

        verify(stockRepository).save(stock);
        assertThat(stock.getCurrentPrice()).isGreaterThanOrEqualTo(BigDecimal.ONE);
    }

    @Test
    void shouldHandleMultipleActiveStocks() {
        Stock stock1 = Stock.builder()
                .id(UUID.randomUUID())
                .symbol("AAPL")
                .currentPrice(new BigDecimal("150"))
                .status(StockStatus.ACTIVE)
                .build();

        Stock stock2 = Stock.builder()
                .id(UUID.randomUUID())
                .symbol("MSFT")
                .currentPrice(new BigDecimal("200"))
                .status(StockStatus.ACTIVE)
                .build();

        when(stockRepository.findByStatus(StockStatus.ACTIVE)).thenReturn(List.of(stock1, stock2));

        fluctuationService.fluctuatePrices();

        verify(stockRepository).save(stock1);
        verify(stockRepository).save(stock2);
    }
}