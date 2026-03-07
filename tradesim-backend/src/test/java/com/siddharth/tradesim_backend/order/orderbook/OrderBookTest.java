package com.siddharth.tradesim_backend.order.orderbook;

import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.order.enums.OrderSide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderBookTest {

    private OrderBook orderBook;

    @BeforeEach
    void setUp() {
        orderBook = new OrderBook();
    }

    private OrderBookEntry createEntry(OrderSide side, int quantity, double price) {
        return new OrderBookEntry(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                side,
                BigDecimal.valueOf(price),
                quantity,
                Instant.now()
        );
    }

    @Test
    void shouldAddBuyOrderToBuyQueue() {
        OrderBookEntry buy = createEntry(OrderSide.BUY, 10, 100);

        orderBook.addOrder(buy);

        assertEquals(1, orderBook.getBuyOrders().size());
        assertEquals(0, orderBook.getSellOrders().size());
    }

    @Test
    void shouldAddSellOrderToSellQueue() {
        OrderBookEntry sell = createEntry(OrderSide.SELL, 10, 100);

        orderBook.addOrder(sell);

        assertEquals(1, orderBook.getSellOrders().size());
        assertEquals(0, orderBook.getBuyOrders().size());
    }

    @Test
    void shouldRemoveOrderById() {
        OrderBookEntry buy = createEntry(OrderSide.BUY, 10, 100);

        orderBook.addOrder(buy);

        orderBook.removeOrder(buy.orderId());

        assertTrue(orderBook.getBuyOrders().isEmpty());
    }

    @Test
    void shouldEstimateBuyCostSingleLevel() {
        OrderBookEntry sell = createEntry(OrderSide.SELL, 10, 100);

        orderBook.addOrder(sell);

        BigDecimal cost = orderBook.estimateBuyCost(5);

        assertEquals(0, cost.compareTo(BigDecimal.valueOf(500)));
    }

    @Test
    void shouldEstimateBuyCostAcrossMultipleLevels() {
        OrderBookEntry sell1 = createEntry(OrderSide.SELL, 5, 100);
        OrderBookEntry sell2 = createEntry(OrderSide.SELL, 5, 101);

        orderBook.addOrder(sell1);
        orderBook.addOrder(sell2);

        BigDecimal cost = orderBook.estimateBuyCost(8);

        assertEquals(0, cost.compareTo(BigDecimal.valueOf(803)));
    }

    @Test
    void shouldThrowExceptionWhenNoLiquidity() {
        assertThrows(BusinessException.class, () -> orderBook.estimateBuyCost(5));
    }

    @Test
    void shouldThrowExceptionForInvalidQuantity() {
        assertThrows(IllegalArgumentException.class, () -> orderBook.estimateBuyCost(0));
    }
}