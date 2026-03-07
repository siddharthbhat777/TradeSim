package com.siddharth.tradesim_backend.order.service;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.holding.HoldingRepository;
import com.siddharth.tradesim_backend.holding.model.Holding;
import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.model.dto.OrderRequest;
import com.siddharth.tradesim_backend.order.orderbook.MatchResult;
import com.siddharth.tradesim_backend.order.orderbook.OrderBookManager;
import com.siddharth.tradesim_backend.order.orderbook.OrderMatchingEngine;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import com.siddharth.tradesim_backend.stock.StockRepository;
import com.siddharth.tradesim_backend.stock.enums.StockStatus;
import com.siddharth.tradesim_backend.stock.model.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {
    private OrderService orderService;

    private AuthRepository authRepository;
    private StockRepository stockRepository;
    private OrderRepository orderRepository;
    private HoldingRepository holdingRepository;
    private OrderBookManager orderBookManager;
    private OrderMatchingEngine orderMatchingEngine;

    private UUID userId;
    private UUID stockId;

    @BeforeEach
    void setup() {
        authRepository = mock(AuthRepository.class);
        stockRepository = mock(StockRepository.class);
        orderRepository = mock(OrderRepository.class);
        holdingRepository = mock(HoldingRepository.class);
        orderBookManager = mock(OrderBookManager.class);
        orderMatchingEngine = mock(OrderMatchingEngine.class);

        orderService = new OrderService(
                authRepository,
                stockRepository,
                orderRepository,
                holdingRepository,
                orderBookManager,
                orderMatchingEngine
        );

        userId = UUID.randomUUID();
        stockId = UUID.randomUUID();
    }

    private OrderRequest createLimitBuyRequest() {
        OrderRequest request = new OrderRequest();

        ReflectionTestUtils.setField(request, "stockId", stockId);
        ReflectionTestUtils.setField(request, "quantity", 10);
        ReflectionTestUtils.setField(request, "side", OrderSide.BUY);
        ReflectionTestUtils.setField(request, "orderType", OrderType.LIMIT);
        ReflectionTestUtils.setField(request, "limitPrice", BigDecimal.valueOf(100));

        return request;
    }

    @Test
    void shouldCreateLimitBuyOrder() {
        User user = mock(User.class);
        Stock stock = mock(Stock.class);

        when(user.getAccountStatus()).thenReturn(AccountStatus.ACTIVE);
        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(stock.getStatus()).thenReturn(StockStatus.ACTIVE);
        when(stock.getId()).thenReturn(stockId);
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(orderMatchingEngine.match(any())).thenReturn(new MatchResult(false));

        OrderRequest request = createLimitBuyRequest();

        orderService.createOrder(userId, request);

        verify(user).lockFunds(BigDecimal.valueOf(1000));
        verify(orderRepository).save(any());
        verify(orderBookManager).addOrderToOrderBook(any());
        verify(orderBookManager).registerOrder(any());
        verify(orderMatchingEngine).match(any());
    }

    @Test
    void shouldCreateLimitSellOrder() {
        User user = mock(User.class);
        Stock stock = mock(Stock.class);
        Holding holding = mock(Holding.class);

        when(user.getAccountStatus()).thenReturn(AccountStatus.ACTIVE);
        when(user.getId()).thenReturn(userId);
        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(stock.getStatus()).thenReturn(StockStatus.ACTIVE);
        when(stock.getId()).thenReturn(stockId);
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));
        when(holdingRepository.findByUserIdAndStockId(userId, stockId)).thenReturn(Optional.of(holding));
        when(orderMatchingEngine.match(any())).thenReturn(new MatchResult(false));

        OrderRequest request = new OrderRequest();

        ReflectionTestUtils.setField(request, "stockId", stockId);
        ReflectionTestUtils.setField(request, "quantity", 5);
        ReflectionTestUtils.setField(request, "side", OrderSide.SELL);
        ReflectionTestUtils.setField(request, "orderType", OrderType.LIMIT);
        ReflectionTestUtils.setField(request, "limitPrice", BigDecimal.valueOf(100));

        orderService.createOrder(userId, request);

        verify(holding).lockShares(5);
        verify(holdingRepository).save(holding);
    }

    @Test
    void shouldRejectInactiveStock() {
        User user = mock(User.class);
        Stock stock = mock(Stock.class);

        when(user.getAccountStatus()).thenReturn(AccountStatus.ACTIVE);
        when(authRepository.findById(userId)).thenReturn(Optional.of(user));
        when(stock.getStatus()).thenReturn(StockStatus.HALTED);
        when(stockRepository.findById(stockId)).thenReturn(Optional.of(stock));

        OrderRequest request = createLimitBuyRequest();

        assertThrows(BusinessException.class, () -> orderService.createOrder(userId, request));
    }

}