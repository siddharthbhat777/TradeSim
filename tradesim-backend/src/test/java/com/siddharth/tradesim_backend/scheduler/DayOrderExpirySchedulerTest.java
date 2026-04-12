package com.siddharth.tradesim_backend.scheduler;

import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.enums.TimeInForce;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import com.siddharth.tradesim_backend.order.service.OrderLifecycleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DayOrderExpirySchedulerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderLifecycleService orderLifecycleService;

    @InjectMocks
    private DayOrderExpiryScheduler scheduler;

    @Test
    void shouldCancelExpiredOrders() {
        Order openOrder = Order.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .stockId(UUID.randomUUID())
                .side(OrderSide.BUY)
                .orderType(OrderType.LIMIT)
                .timeInForce(TimeInForce.DAY)
                .quantity(10)
                .remainingQuantity(10)
                .status(OrderStatus.OPEN)
                .expiresAt(Instant.now().minusSeconds(5))
                .build();

        Order partialOrder = Order.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .stockId(UUID.randomUUID())
                .side(OrderSide.SELL)
                .orderType(OrderType.MARKET)
                .timeInForce(TimeInForce.DAY)
                .quantity(10)
                .remainingQuantity(5)
                .status(OrderStatus.PARTIALLY_FILLED)
                .expiresAt(Instant.now().minusSeconds(5))
                .build();

        when(orderRepository.findByStatusInAndExpiresAtLessThanEqual(eq(List.of(OrderStatus.OPEN, OrderStatus.PARTIALLY_FILLED)), any(Instant.class))).thenReturn(List.of(openOrder, partialOrder));

        scheduler.cancelExpiredDayOrders();

        verify(orderLifecycleService, times(1)).cancelOrder(openOrder);
        verify(orderLifecycleService, times(1)).cancelOrder(partialOrder);
    }
}