package com.siddharth.tradesim_backend.order.orderbook;

import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.model.Fill;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.repository.FillRepository;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import com.siddharth.tradesim_backend.portfolio.model.dto.TradeExecution;
import com.siddharth.tradesim_backend.portfolio.service.PortfolioService;
import com.siddharth.tradesim_backend.stock.service.MarketStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
    public MatchResult match(Order order) {
        return orderBookManager.withLock(order.getStockId(), orderBook -> {
            boolean priceBandHit = false;
            boolean executedSomething = false;
            if (order.getOrderType() == OrderType.MARKET) {
                switch (order.getSide()) {
                    case BUY -> priceBandHit = matchBuy(order, orderBook);
                    case SELL -> priceBandHit = matchSell(order, orderBook);
                }

                resolveMarketFinalStatus(order);
                orderRepository.save(order);
                return new MatchResult(priceBandHit);
            }

            List<OrderBookEntry> skipped = new ArrayList<>();
            while (!orderBook.getBuyOrders().isEmpty() && !orderBook.getSellOrders().isEmpty()) {
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
                if (cannotExecute(order.getStockId(), executionPrice)) {
                    priceBandHit = true;
                    skipped.add(orderBook.getSellOrders().poll());
                    continue;
                }

                if (buyOrder.getUserId().equals(sellOrder.getUserId())) {
                    skipped.add(orderBook.getSellOrders().poll());
                    continue;
                }

                executedSomething = true;

                executeTrade(buyOrder, sellOrder, executedQuantity, executionPrice);

                orderBook.updateOrder(buyEntry, executedQuantity);
                orderBook.updateOrder(sellEntry, executedQuantity);
            }

            skipped.forEach(orderBook::addOrder);
            orderRepository.save(order);
            return new MatchResult(priceBandHit && !executedSomething);
        });
    }

    private boolean matchBuy(Order order, OrderBook orderBook) {
        boolean bandEncountered = false;
        boolean executedSomething = false;

        List<OrderBookEntry> skipped = new ArrayList<>();
        while (order.getRemainingQuantity() > 0 && !orderBook.getSellOrders().isEmpty()) {
            OrderBookEntry sellEntry = orderBook.getSellOrders().peek();
            Order sellOrder = orderBookManager.getOrderOrLoad(sellEntry.orderId());
            if (sellOrder == null) {
                orderBook.getSellOrders().poll();
                continue;
            }

            int executedQuantity = Math.min(order.getRemainingQuantity(), sellOrder.getRemainingQuantity());
            if (executedQuantity <= 0) {
                break;
            }

            BigDecimal executionPrice = sellEntry.price();
            if (cannotExecute(order.getStockId(), executionPrice)) {
                bandEncountered = true;
                skipped.add(orderBook.getSellOrders().poll());
                continue;
            }

            if (order.getUserId().equals(sellOrder.getUserId())) {
                skipped.add(orderBook.getSellOrders().poll());
                continue;
            }

            executedSomething = true;

            executeTrade(order, sellOrder, executedQuantity, sellEntry.price());

            orderBook.updateOrder(sellEntry, executedQuantity);
        }

        skipped.forEach(orderBook::addOrder);
        return bandEncountered && !executedSomething;
    }

    private boolean matchSell(Order order, OrderBook orderBook) {
        boolean bandEncountered = false;
        boolean executedSomething = false;

        List<OrderBookEntry> skipped = new ArrayList<>();
        while (order.getRemainingQuantity() > 0 && !orderBook.getBuyOrders().isEmpty()) {
            OrderBookEntry buyEntry = orderBook.getBuyOrders().peek();
            Order buyOrder = orderBookManager.getOrderOrLoad(buyEntry.orderId());
            if (buyOrder == null) {
                orderBook.getBuyOrders().poll();
                continue;
            }

            int executedQuantity = Math.min(buyOrder.getRemainingQuantity(), order.getRemainingQuantity());
            if (executedQuantity <= 0) {
                break;
            }

            BigDecimal executionPrice = buyEntry.price();
            if (cannotExecute(order.getStockId(), executionPrice)) {
                bandEncountered = true;
                skipped.add(orderBook.getBuyOrders().poll());
                continue;
            }

            if (buyOrder.getUserId().equals(order.getUserId())) {
                skipped.add(orderBook.getBuyOrders().poll());
                continue;
            }

            executedSomething = true;

            executeTrade(buyOrder, order, executedQuantity, buyEntry.price());

            orderBook.updateOrder(buyEntry, executedQuantity);
        }

        skipped.forEach(orderBook::addOrder);
        return bandEncountered && !executedSomething;
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
            orderBookManager.removeOrder(buyOrder);
        }

        if (sellOrder.getStatus() == OrderStatus.FILLED) {
            orderBookManager.removeOrder(sellOrder);
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

    private boolean cannotExecute(UUID stockId, BigDecimal executionPrice) {
        return !marketStateService.isWithinPriceBand(stockId, executionPrice);
    }
}