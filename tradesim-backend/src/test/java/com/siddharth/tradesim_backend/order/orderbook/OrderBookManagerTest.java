package com.siddharth.tradesim_backend.order.orderbook;

import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
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
                .quantity(10)
                .remainingQuantity(10)
                .limitPrice(BigDecimal.valueOf((double) 100))
                .status(OrderStatus.OPEN)
                .build();

        ReflectionTestUtils.setField(order, "createdAt", Instant.now());

        return order;
    }

    @Test
    void shouldRegisterAndRetrieveOrder() {
        Order order = createOrder(OrderSide.BUY);

        orderBookManager.registerOrder(order);

        Order stored = orderBookManager.getOrder(order.getId());

        assertNotNull(stored);
        assertEquals(order.getId(), stored.getId());
    }

    @Test
    void shouldAddOrderToCorrectOrderBook() {
        Order order = createOrder(OrderSide.BUY);

        orderBookManager.registerOrder(order);
        orderBookManager.addOrderToOrderBook(order);

        OrderBook orderBook = orderBookManager.getOrderBook(stockId);

        assertEquals(1, orderBook.getBuyOrders().size());
        assertEquals(0, orderBook.getSellOrders().size());
    }

    @Test
    void shouldRemoveOrderFromOrderBook() {
        Order order = createOrder(OrderSide.SELL);

        orderBookManager.registerOrder(order);
        orderBookManager.addOrderToOrderBook(order);

        orderBookManager.removeOrderFromOrderBook(order);

        OrderBook orderBook = orderBookManager.getOrderBook(stockId);

        assertEquals(0, orderBook.getSellOrders().size());
    }

    @Test
    void shouldUnregisterOrder() {
        Order order = createOrder(OrderSide.BUY);

        orderBookManager.registerOrder(order);

        orderBookManager.unregisterOrder(order.getId());

        Order stored = orderBookManager.getOrder(order.getId());

        assertNull(stored);
    }

    @Test
    void shouldCreateOrderBookIfNotExists() {
        OrderBook book1 = orderBookManager.getOrderBook(stockId);
        OrderBook book2 = orderBookManager.getOrderBook(stockId);

        assertSame(book1, book2);
    }
}