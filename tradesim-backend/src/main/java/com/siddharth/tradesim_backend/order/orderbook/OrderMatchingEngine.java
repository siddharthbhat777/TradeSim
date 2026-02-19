package com.siddharth.tradesim_backend.order.orderbook;

import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.model.Fill;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.repository.FillRepository;
import com.siddharth.tradesim_backend.portfolio.PortfolioService;
import com.siddharth.tradesim_backend.portfolio.dto.TradeExecution;
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

        while (!orderBook.getBuyOrders().isEmpty() && !orderBook.getSellOrders().isEmpty()) {
            OrderBookEntry buyEntry = orderBook.getBuyOrders().peek();
            OrderBookEntry sellEntry = orderBook.getSellOrders().peek();

            if (buyEntry.price().compareTo(sellEntry.price()) < 0) {
                break;
            }

            Order buyOrder = Objects.requireNonNull(orderBookManager.getOrder(buyEntry.orderId()), "Buy order not found in memory");
            Order sellOrder = Objects.requireNonNull(orderBookManager.getOrder(sellEntry.orderId()), "Sell order not found in memory");

            int executedQuantity = Math.min(buyOrder.getRemainingQuantity(), sellOrder.getRemainingQuantity());

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
    }

    private void matchBuy(Order order, OrderBook orderBook) {
        while (order.getRemainingQuantity() > 0 && !orderBook.getSellOrders().isEmpty()) {
            OrderBookEntry sellEntry = orderBook.getSellOrders().peek();
            Order sellOrder = Objects.requireNonNull(orderBookManager.getOrder(sellEntry.orderId()), "Sell order not found in memory");

            int executedQuantity = Math.min(order.getRemainingQuantity(), sellOrder.getRemainingQuantity());

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
        buyOrder.fillOrderQuantity(executedQuantity);
        sellOrder.fillOrderQuantity(executedQuantity);

        updateStatus(buyOrder);
        updateStatus(sellOrder);

        if (buyOrder.getStatus() == OrderStatus.FILLED) {
            orderBookManager.unregisterOrder(buyOrder.getId());
        }

        if (sellOrder.getStatus() == OrderStatus.FILLED) {
            orderBookManager.unregisterOrder(sellOrder.getId());
        }

        if (executedQuantity > 0) {
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
                    executionPrice
            );
            portfolioService.settleTrade(execution);
            orderRepository.save(buyOrder);
            orderRepository.save(sellOrder);
            fillRepository.save(fillOrder);
        }
    }

    private void updateStatus(Order order) {
        if (order.getRemainingQuantity() == 0) {
            order.setStatus(OrderStatus.FILLED);
        } else {
            order.setStatus(OrderStatus.PARTIALLY_FILLED);
        }
    }

    private void resolveMarketFinalStatus(Order order) {
        if (order.getRemainingQuantity() == order.getQuantity()) {
            order.setStatus(OrderStatus.CANCELLED);
        }
    }
}
