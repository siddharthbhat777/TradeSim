package com.siddharth.tradesim_backend.order.service;

import com.siddharth.tradesim_backend.ledger.LedgerService;
import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.orderbook.OrderBookManager;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import com.siddharth.tradesim_backend.position.PositionRepository;
import com.siddharth.tradesim_backend.position.model.Position;
import com.siddharth.tradesim_backend.trading_account.TradingAccountService;
import com.siddharth.tradesim_backend.trading_account.model.TradingAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class OrderLifecycleServiceTest {
    private OrderLifecycleService service;
    private OrderRepository orderRepository;
    private OrderBookManager orderBookManager;
    private TradingAccountService tradingAccountService;
    private PositionRepository positionRepository;
    private LedgerService ledgerService;

    private UUID userId;
    private UUID stockId;

    @BeforeEach
    void setup() {
        orderRepository = mock(OrderRepository.class);
        orderBookManager = mock(OrderBookManager.class);
        tradingAccountService = mock(TradingAccountService.class);
        positionRepository = mock(PositionRepository.class);
        ledgerService = mock(LedgerService.class);

        service = new OrderLifecycleService(
                orderRepository,
                orderBookManager,
                tradingAccountService,
                positionRepository,
                ledgerService
        );

        userId = UUID.randomUUID();
        stockId = UUID.randomUUID();

        ReentrantLock lock = new ReentrantLock();
        when(orderBookManager.getLock(any())).thenReturn(lock);
    }

    private Order createOrder(OrderSide side, OrderType type, int qty, double price) {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .stockId(stockId)
                .side(side)
                .orderType(type)
                .quantity(qty)
                .remainingQuantity(qty)
                .limitPrice(price == 0 ? null : BigDecimal.valueOf(price))
                .status(OrderStatus.OPEN)
                .build();

        ReflectionTestUtils.setField(order, "createdAt", Instant.now());

        return order;
    }

    @Test
    void shouldCancelBuyLimitOrderAndUnlockFunds() {
        Order order = createOrder(OrderSide.BUY, OrderType.LIMIT, 10, 100);

        TradingAccount tradingAccount = mock(TradingAccount.class);
        when(tradingAccountService.getTradingAccountByUserId(userId)).thenReturn(tradingAccount);
        when(tradingAccount.getLeverage()).thenReturn(5);

        service.cancelOrder(order);

        verify(tradingAccount).unlockFunds(argThat(amount -> amount.compareTo(BigDecimal.valueOf(200)) == 0));
        verify(tradingAccountService).saveTradingAccount(tradingAccount);
        verify(orderBookManager).removeOrder(order);
        verify(orderRepository).save(order);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(ledgerService).recordBuyLimitMarginUnlock(eq(tradingAccount), argThat(amount -> amount.compareTo(BigDecimal.valueOf(200)) == 0), eq(stockId), any());
    }

    @Test
    void shouldCancelSellOrderAndUnlockShares() {
        Order order = createOrder(OrderSide.SELL, OrderType.LIMIT, 5, 100);

        Position position = mock(Position.class);

        when(positionRepository.findByUserIdAndStockId(userId, stockId)).thenReturn(Optional.of(position));

        service.cancelOrder(order);

        verify(position).unlockShares(5);
        verify(orderBookManager).removeOrder(order);
        verify(orderRepository).save(order);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void shouldIgnoreMarketBuyUnlock() {
        Order order = createOrder(OrderSide.BUY, OrderType.MARKET, 10, 0);

        service.cancelOrder(order);

        verify(tradingAccountService, never()).getTradingAccountByUserId(any());
        verify(orderBookManager).removeOrder(order);
        verify(orderRepository).save(order);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void shouldNotCancelFilledOrder() {
        Order order = createOrder(OrderSide.BUY, OrderType.LIMIT, 10, 100);

        ReflectionTestUtils.setField(order, "status", OrderStatus.FILLED);

        service.cancelOrder(order);

        verify(orderRepository, never()).save(any());
        verify(orderBookManager, never()).removeOrder(any());
    }

    @Test
    void shouldNotCancelAlreadyCancelledOrder() {
        Order order = createOrder(OrderSide.BUY, OrderType.LIMIT, 10, 100);

        ReflectionTestUtils.setField(order, "status", OrderStatus.CANCELLED);

        service.cancelOrder(order);

        verify(orderRepository, never()).save(any());
        verify(orderBookManager, never()).removeOrder(any());
    }
}