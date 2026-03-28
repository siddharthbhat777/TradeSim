package com.siddharth.tradesim_backend.order.orderbook;

import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.repository.FillRepository;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import com.siddharth.tradesim_backend.portfolio.service.PortfolioService;
import com.siddharth.tradesim_backend.stock.service.MarketStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderMatchingEngineTest {
    private OrderMatchingEngine engine;
    private OrderBookManager orderBookManager;

    private FillRepository fillRepository;
    private PortfolioService portfolioService;
    private MarketStateService marketStateService;

    private UUID stockId;

    @BeforeEach
    void setup() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        fillRepository = mock(FillRepository.class);
        portfolioService = mock(PortfolioService.class);
        marketStateService = mock(MarketStateService.class);

        orderBookManager = new OrderBookManager(orderRepository);

        engine = new OrderMatchingEngine(
                orderBookManager,
                orderRepository,
                fillRepository,
                portfolioService,
                marketStateService
        );

        stockId = UUID.randomUUID();

        when(marketStateService.isWithinPriceBand(any(), any())).thenReturn(true);
    }

    private Order createOrder(OrderSide side, OrderType type, int quantity, double price) {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .stockId(stockId)
                .side(side)
                .orderType(type)
                .quantity(quantity)
                .remainingQuantity(quantity)
                .limitPrice(price == 0 ? null : BigDecimal.valueOf(price))
                .status(OrderStatus.OPEN)
                .build();

        ReflectionTestUtils.setField(order, "createdAt", Instant.now());

        return order;
    }

    @Test
    void shouldFullyMatchLimitOrders() {
        Order sell = createOrder(OrderSide.SELL, OrderType.LIMIT, 10, 100);
        orderBookManager.addOrder(sell);

        Order buy = createOrder(OrderSide.BUY, OrderType.LIMIT, 10, 100);
        orderBookManager.addOrder(buy);
        engine.match(buy);

        assertEquals(OrderStatus.FILLED, buy.getStatus());
        assertEquals(OrderStatus.FILLED, sell.getStatus());
        verify(fillRepository, times(1)).save(any());
        verify(portfolioService, times(1)).settleTrade(any());
    }

    @Test
    void shouldPartiallyFillOrder() {
        Order sell = createOrder(OrderSide.SELL, OrderType.LIMIT, 5, 100);
        orderBookManager.addOrder(sell);

        Order buy = createOrder(OrderSide.BUY, OrderType.LIMIT, 10, 100);
        orderBookManager.addOrder(buy);
        engine.match(buy);

        assertEquals(OrderStatus.PARTIALLY_FILLED, buy.getStatus());
        assertEquals(OrderStatus.FILLED, sell.getStatus());
        assertEquals(5, buy.getRemainingQuantity());
    }

    @Test
    void shouldRespectPricePriority() {
        Order sell1 = createOrder(OrderSide.SELL, OrderType.LIMIT, 5, 100);
        Order sell2 = createOrder(OrderSide.SELL, OrderType.LIMIT, 5, 101);

        orderBookManager.addOrder(sell1);
        orderBookManager.addOrder(sell2);

        Order buy = createOrder(OrderSide.BUY, OrderType.LIMIT, 5, 105);
        orderBookManager.addOrder(buy);
        engine.match(buy);

        assertEquals(OrderStatus.FILLED, buy.getStatus());
        assertEquals(OrderStatus.FILLED, sell1.getStatus());
        assertEquals(OrderStatus.OPEN, sell2.getStatus());
    }

    @Test
    void shouldRespectTimePriority() throws InterruptedException {
        Order sell1 = createOrder(OrderSide.SELL, OrderType.LIMIT, 5, 100);
        Thread.sleep(1);
        Order sell2 = createOrder(OrderSide.SELL, OrderType.LIMIT, 5, 100);

        orderBookManager.addOrder(sell1);
        orderBookManager.addOrder(sell2);

        Order buy = createOrder(OrderSide.BUY, OrderType.LIMIT, 5, 100);
        orderBookManager.addOrder(buy);
        engine.match(buy);

        assertEquals(OrderStatus.FILLED, sell1.getStatus());
        assertEquals(OrderStatus.OPEN, sell2.getStatus());
    }

    @Test
    void shouldSweepMultiplePriceLevels() {
        Order sell1 = createOrder(OrderSide.SELL, OrderType.LIMIT, 5, 100);
        Order sell2 = createOrder(OrderSide.SELL, OrderType.LIMIT, 5, 101);

        orderBookManager.addOrder(sell1);
        orderBookManager.addOrder(sell2);

        Order buy = createOrder(OrderSide.BUY, OrderType.MARKET, 8, 0);
        orderBookManager.addOrder(buy);
        engine.match(buy);

        assertEquals(OrderStatus.FILLED, sell1.getStatus());
        assertEquals(OrderStatus.PARTIALLY_FILLED, sell2.getStatus());
        assertEquals(2, sell2.getRemainingQuantity());
    }

    @Test
    void shouldPreventSelfTrade() {
        UUID userId = UUID.randomUUID();

        Order sell = Order.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .stockId(stockId)
                .side(OrderSide.SELL)
                .orderType(OrderType.LIMIT)
                .quantity(10)
                .remainingQuantity(10)
                .limitPrice(BigDecimal.valueOf(100))
                .status(OrderStatus.OPEN)
                .build();

        orderBookManager.addOrder(sell);

        Order buy = Order.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .stockId(stockId)
                .side(OrderSide.BUY)
                .orderType(OrderType.LIMIT)
                .quantity(10)
                .remainingQuantity(10)
                .limitPrice(BigDecimal.valueOf(100))
                .status(OrderStatus.OPEN)
                .build();

        orderBookManager.addOrder(buy);
        engine.match(buy);

        assertEquals(OrderStatus.OPEN, buy.getStatus());
        assertEquals(OrderStatus.OPEN, sell.getStatus());
    }

    @Test
    void shouldStopMatchingIfPriceBandHit() {
        when(marketStateService.isWithinPriceBand(any(), any())).thenReturn(false);

        Order sell = createOrder(OrderSide.SELL, OrderType.LIMIT, 10, 100);
        orderBookManager.addOrder(sell);

        Order buy = createOrder(OrderSide.BUY, OrderType.LIMIT, 10, 100);
        orderBookManager.addOrder(buy);
        MatchResult result = engine.match(buy);

        assertTrue(result.priceBandHit());
    }
}