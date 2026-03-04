package com.siddharth.tradesim_backend.stock.service;

import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import com.siddharth.tradesim_backend.order.service.OrderLifecycleService;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.enums.StockStatus;
import com.siddharth.tradesim_backend.stock.exceptions.StockStatusException;
import com.siddharth.tradesim_backend.stock.model.Stock;
import com.siddharth.tradesim_backend.stock.model.dto.StockResponse;
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
    private OrderRepository orderRepository;

    @Mock
    private OrderLifecycleService orderLifecycleService;

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
    void shouldCancelAllOpenAndPartiallyFilledOrdersWhenStockIsDelisted() {
        UUID stockId = UUID.randomUUID();

        Stock stock = Stock.builder()
                .id(stockId)
                .status(StockStatus.ACTIVE)
                .build();

        Order openOrder = Order.builder()
                .status(OrderStatus.OPEN)
                .build();

        Order partialOrder = Order.builder()
                .status(OrderStatus.PARTIALLY_FILLED)
                .build();

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(orderRepository.findByStockIdAndStatusIn(eq(stockId), eq(List.of(OrderStatus.OPEN, OrderStatus.PARTIALLY_FILLED)))).thenReturn(List.of(openOrder, partialOrder));

        when(stockRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        stockService.changeStockStatus(stockId, StockStatus.DELISTED);

        verify(orderLifecycleService).cancelOrder(openOrder);
        verify(orderLifecycleService).cancelOrder(partialOrder);
        verify(stockRepository).save(stock);
    }

    @Test
    void shouldNotCancelAnythingWhenNoOpenOrdersExist() {
        UUID stockId = UUID.randomUUID();

        Stock stock = Stock.builder()
                .id(stockId)
                .status(StockStatus.ACTIVE)
                .build();

        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(orderRepository.findByStockIdAndStatusIn(eq(stockId), eq(List.of(OrderStatus.OPEN, OrderStatus.PARTIALLY_FILLED)))).thenReturn(List.of());

        when(stockRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        stockService.changeStockStatus(stockId, StockStatus.DELISTED);

        verify(orderLifecycleService, never()).cancelOrder(any());
    }
}