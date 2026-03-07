package com.siddharth.tradesim_backend.order.service;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.holding.HoldingRepository;
import com.siddharth.tradesim_backend.holding.model.Holding;
import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.orderbook.OrderBookManager;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderLifecycleServiceTest {
    private OrderLifecycleService service;

    private OrderRepository orderRepository;
    private OrderBookManager orderBookManager;
    private AuthRepository authRepository;
    private HoldingRepository holdingRepository;

    private UUID userId;
    private UUID stockId;

    @BeforeEach
    void setup() {
        orderRepository = mock(OrderRepository.class);
        orderBookManager = mock(OrderBookManager.class);
        authRepository = mock(AuthRepository.class);
        holdingRepository = mock(HoldingRepository.class);

        service = new OrderLifecycleService(
                orderRepository,
                orderBookManager,
                authRepository,
                holdingRepository
        );

        userId = UUID.randomUUID();
        stockId = UUID.randomUUID();
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

        User user = mock(User.class);
        when(authRepository.findById(userId)).thenReturn(Optional.of(user));

        service.cancelOrder(order);

        verify(user).unlockFunds(BigDecimal.valueOf(1000.0));
        verify(orderBookManager).unregisterOrder(order.getId());
        verify(orderRepository).save(order);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void shouldCancelSellOrderAndUnlockShares() {
        Order order = createOrder(OrderSide.SELL, OrderType.LIMIT, 5, 100);

        Holding holding = mock(Holding.class);

        when(holdingRepository.findByUserIdAndStockId(userId, stockId)).thenReturn(Optional.of(holding));

        service.cancelOrder(order);

        verify(holding).unlockShares(5);
        verify(orderBookManager).unregisterOrder(order.getId());
        verify(orderRepository).save(order);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void shouldIgnoreMarketBuyUnlock() {
        Order order = createOrder(OrderSide.BUY, OrderType.MARKET, 10, 0);

        service.cancelOrder(order);

        verify(authRepository, never()).findById(any());
        verify(orderBookManager).unregisterOrder(order.getId());
        verify(orderRepository).save(order);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void shouldNotCancelFilledOrder() {
        Order order = createOrder(OrderSide.BUY, OrderType.LIMIT, 10, 100);

        ReflectionTestUtils.setField(order, "status", OrderStatus.FILLED);

        service.cancelOrder(order);

        verify(orderRepository, never()).save(any());
        verify(orderBookManager, never()).unregisterOrder(any());
    }

    @Test
    void shouldNotCancelAlreadyCancelledOrder() {
        Order order = createOrder(OrderSide.BUY, OrderType.LIMIT, 10, 100);

        ReflectionTestUtils.setField(order, "status", OrderStatus.CANCELLED);

        service.cancelOrder(order);

        verify(orderRepository, never()).save(any());
        verify(orderBookManager, never()).unregisterOrder(any());
    }
}