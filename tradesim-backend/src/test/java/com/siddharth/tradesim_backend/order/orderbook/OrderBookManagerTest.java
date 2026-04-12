package com.siddharth.tradesim_backend.order.orderbook;

import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.enums.TimeInForce;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class OrderBookManagerTest {
    private OrderBookManager orderBookManager;
    private UUID stockId;

    @BeforeEach
    void setup() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        orderBookManager = new OrderBookManager(orderRepository);
        stockId = UUID.randomUUID();
    }

    private Order createOrder(OrderSide side) {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .stockId(stockId)
                .side(side)
                .orderType(OrderType.LIMIT)
                .timeInForce(TimeInForce.DAY)
                .quantity(10)
                .remainingQuantity(10)
                .limitPrice(BigDecimal.valueOf(100))
                .reservationPrice(side == OrderSide.BUY ? BigDecimal.valueOf(100) : null)
                .bookPrice(BigDecimal.valueOf(100))
                .status(OrderStatus.OPEN)
                .build();

        ReflectionTestUtils.setField(order, "createdAt", Instant.now());
        return order;
    }

    @Test
    void shouldAddOrderToBookAndMemory() {
        Order order = createOrder(OrderSide.BUY);

        orderBookManager.addOrder(order);

        Order stored = orderBookManager.getOrder(order.getId());
        OrderBook orderBook = orderBookManager.getOrderBook(stockId);

        assertNotNull(stored);
        assertEquals(order.getId(), stored.getId());
        assertEquals(1, orderBook.getBuyOrders().size());
        assertEquals(0, orderBook.getSellOrders().size());
    }

    @Test
    void shouldRemoveOrderFromBookAndMemory() {
        Order order = createOrder(OrderSide.SELL);

        orderBookManager.addOrder(order);
        orderBookManager.removeOrder(order);

        OrderBook orderBook = orderBookManager.getOrderBook(stockId);

        assertEquals(0, orderBook.getSellOrders().size());
        assertNull(orderBookManager.getOrder(order.getId()));
    }

    @Test
    void shouldCreateOrderBookIfNotExists() {
        OrderBook book1 = orderBookManager.getOrderBook(stockId);
        OrderBook book2 = orderBookManager.getOrderBook(stockId);

        assertSame(book1, book2);
    }
}