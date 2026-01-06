package com.siddharth.tradesim_backend.stock.service;

import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.enums.StockStatus;
import com.siddharth.tradesim_backend.stock.exceptions.StockStatusException;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.stock.model.dto.StockResponse;
import com.siddharth.tradesim_backend.trade.TradeRepository;
import com.siddharth.tradesim_backend.trade.enums.Status;
import com.siddharth.tradesim_backend.trade.model.Trade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StockServiceTest {

    @Mock
    private StockRepository stockRepository;
    @Mock
    private TradeRepository tradeRepository;

    @InjectMocks
    private StockService stockService;

    @Test
    void shouldChangeStockStatusWhenValid() {
        UUID stockId = UUID.randomUUID();

        Stock stock = Stock.builder()
                .id(stockId)
                .status(StockStatus.ACTIVE)
                .build();

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(stockRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        StockResponse response = stockService.changeStockStatus(stockId, StockStatus.HALTED);

        assertThat(response.status()).isEqualTo(StockStatus.HALTED);
    }

    @Test
    void shouldThrowExceptionWhenStockIsAlreadyDelisted() {
        UUID stockId = UUID.randomUUID();

        Stock stock = Stock.builder()
                .id(stockId)
                .status(StockStatus.DELISTED)
                .build();

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));

        assertThrows(StockStatusException.class, () -> stockService.changeStockStatus(stockId, StockStatus.ACTIVE));

        verify(stockRepository, never()).save(any());
    }

    @Test
    void shouldCancelAllPendingTradesWhenStockIsDelisted() {
        UUID stockId = UUID.randomUUID();

        Stock stock = Stock.builder()
                .id(stockId)
                .status(StockStatus.ACTIVE)
                .build();

        Trade trade1 = Trade.builder().status(Status.PENDING).build();
        Trade trade2 = Trade.builder().status(Status.PENDING).build();

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(tradeRepository.findByStockIdAndStatus(stockId, Status.PENDING)).thenReturn(List.of(trade1, trade2));
        when(stockRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        stockService.changeStockStatus(stockId, StockStatus.DELISTED);

        assertThat(trade1.getStatus()).isEqualTo(Status.CANCELLED);
        assertThat(trade2.getStatus()).isEqualTo(Status.CANCELLED);
    }

    @Test
    void shouldNotTouchNonPendingTradesOnDelist() {
        UUID stockId = UUID.randomUUID();

        Stock stock = Stock.builder()
                .id(stockId)
                .status(StockStatus.ACTIVE)
                .build();

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(tradeRepository.findByStockIdAndStatus(stockId, Status.PENDING)).thenReturn(List.of());
        when(tradeRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        when(stockRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        stockService.changeStockStatus(stockId, StockStatus.DELISTED);

        verify(tradeRepository).findByStockIdAndStatus(stockId, Status.PENDING);
        verify(tradeRepository).saveAll(any());
    }
}