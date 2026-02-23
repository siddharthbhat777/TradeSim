package com.siddharth.tradesim_backend.order.orderbook;

import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.model.Fill;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.repository.FillRepository;
import com.siddharth.tradesim_backend.portfolio.PortfolioService;
import com.siddharth.tradesim_backend.portfolio.dto.TradeExecution;
import com.siddharth.tradesim_backend.stock.service.MarketStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderMatchingEngine {
    private final OrderBookManager orderBookManager;
    private final OrderRepository orderRepository;
    private final FillRepository fillRepository;
    private final PortfolioService portfolioService;
    private final MarketStateService marketStateService;

    @Transactional
    public void match(Order order) {
        OrderBook orderBook = orderBookManager.getOrderBook(order.getStockId());
        if (order.getOrderType() == OrderType.MARKET) {
            switch (order.getSide()) {
                case BUY -> matchBuy(order, orderBook);
                case SELL -> matchSell(order, orderBook);
            }

            resolveMarketFinalStatus(order);
            orderRepository.save(order);
            return;
        }

        while (order.getRemainingQuantity() > 0 && !orderBook.getBuyOrders().isEmpty() && !orderBook.getSellOrders().isEmpty()) {
            OrderBookEntry buyEntry = orderBook.getBuyOrders().peek();
            OrderBookEntry sellEntry = orderBook.getSellOrders().peek();

            if (buyEntry.price().compareTo(sellEntry.price()) < 0) {
                break;
            }

            Order buyOrder = Objects.requireNonNull(orderBookManager.getOrder(buyEntry.orderId()), "Buy order not found in memory");
            Order sellOrder = Objects.requireNonNull(orderBookManager.getOrder(sellEntry.orderId()), "Sell order not found in memory");

            int executedQuantity = Math.min(buyOrder.getRemainingQuantity(), sellOrder.getRemainingQuantity());
            if (executedQuantity <= 0) {
                break;
            }

            BigDecimal executionPrice = sellEntry.price();

            executeTrade(buyOrder, sellOrder, executedQuantity, executionPrice);

            orderBook.getBuyOrders().poll();
            orderBook.getSellOrders().poll();

            if (buyEntry.quantity() > executedQuantity) {
                orderBook.getBuyOrders().add(buyEntry.withReducedQuantity(executedQuantity));
            }

            if (sellEntry.quantity() > executedQuantity) {
                orderBook.getSellOrders().add(sellEntry.withReducedQuantity(executedQuantity));
            }
        }
        orderRepository.save(order);
    }

    private void matchBuy(Order order, OrderBook orderBook) {
        while (order.getRemainingQuantity() > 0 && !orderBook.getSellOrders().isEmpty()) {
            OrderBookEntry sellEntry = orderBook.getSellOrders().peek();
            Order sellOrder = Objects.requireNonNull(orderBookManager.getOrder(sellEntry.orderId()), "Sell order not found in memory");

            int executedQuantity = Math.min(order.getRemainingQuantity(), sellOrder.getRemainingQuantity());
            if (executedQuantity <= 0) {
                break;
            }

            executeTrade(order, sellOrder, executedQuantity, sellEntry.price());

            orderBook.getSellOrders().poll();

            if (sellEntry.quantity() > executedQuantity) {
                orderBook.getSellOrders().add(sellEntry.withReducedQuantity(executedQuantity));
            }
        }
    }

    private void matchSell(Order order, OrderBook orderBook) {
        while (order.getRemainingQuantity() > 0 && !orderBook.getBuyOrders().isEmpty()) {
            OrderBookEntry buyEntry = orderBook.getBuyOrders().peek();
            Order buyOrder = Objects.requireNonNull(orderBookManager.getOrder(buyEntry.orderId()), "Buy order not found in memory");

            int executedQuantity = Math.min(buyOrder.getRemainingQuantity(), order.getRemainingQuantity());
            if (executedQuantity <= 0) {
                break;
            }

            executeTrade(buyOrder, order, executedQuantity, buyEntry.price());

            orderBook.getBuyOrders().poll();

            if (buyEntry.quantity() > executedQuantity) {
                orderBook.getBuyOrders().add(buyEntry.withReducedQuantity(executedQuantity));
            }
        }
    }

    private void executeTrade(
            Order buyOrder,
            Order sellOrder,
            int executedQuantity,
            BigDecimal executionPrice
    ) {
        if (buyOrder.getSide() != OrderSide.BUY || sellOrder.getSide() != OrderSide.SELL) {
            throw new IllegalStateException("Invalid trade sides");
        }

        buyOrder.execute(executedQuantity);
        sellOrder.execute(executedQuantity);

        if (buyOrder.getStatus() == OrderStatus.FILLED) {
            orderBookManager.unregisterOrder(buyOrder.getId());
        }

        if (sellOrder.getStatus() == OrderStatus.FILLED) {
            orderBookManager.unregisterOrder(sellOrder.getId());
        }

        Fill fillOrder = Fill.builder()
                .buyOrderId(buyOrder.getId())
                .sellOrderId(sellOrder.getId())
                .stockId(buyOrder.getStockId())
                .quantity(executedQuantity)
                .price(executionPrice)
                .executedAt(Instant.now())
                .build();

        TradeExecution execution = new TradeExecution(
                buyOrder.getUserId(),
                sellOrder.getUserId(),
                buyOrder.getStockId(),
                executedQuantity,
                executionPrice,
                buyOrder.getOrderType(),
                sellOrder.getOrderType(),
                buyOrder.getLimitPrice()
        );
        portfolioService.settleTrade(execution);
        marketStateService.recordTrade(buyOrder.getStockId(), executionPrice, executedQuantity);
        fillRepository.save(fillOrder);
        orderRepository.save(buyOrder);
        orderRepository.save(sellOrder);
    }

    private void resolveMarketFinalStatus(Order order) {
        if (order.getRemainingQuantity() > 0) {
            order.cancel();
        }
    }
}
