package com.siddharth.tradesim_backend.order.orderbook;

import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.model.Fill;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.repository.FillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderMatchingEngine {
    private final OrderBookManager orderBookManager;
    private final OrderRepository orderRepository;
    private final FillRepository fillRepository;

    @Transactional
    public void match(UUID stockId) {
        OrderBook orderBook = orderBookManager.getOrderBook(stockId);

        while (!orderBook.getBuyOrders().isEmpty() && !orderBook.getSellOrders().isEmpty()) {
            OrderBookEntry buyEntry = orderBook.getBuyOrders().peek();
            OrderBookEntry sellEntry = orderBook.getSellOrders().peek();

            log.info("MATCH START stock={}", stockId);
            log.info("BUY={} SELL={}", buyEntry.orderId(), sellEntry.orderId());
            log.info("QTY buy={} sell={}", buyEntry.quantity(), sellEntry.quantity());

            if (buyEntry.price().compareTo(sellEntry.price()) < 0) {
                break;
            }

            int executedQuantity = Math.min(buyEntry.quantity(), sellEntry.quantity());

            BigDecimal executionPrice = sellEntry.price();

            executeTrade(buyEntry, sellEntry, executedQuantity, executionPrice);

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

    private void executeTrade(
            OrderBookEntry buyEntry,
            OrderBookEntry sellEntry,
            int executedQuantity,
            BigDecimal executionPrice
    ) {
        Order buyOrder = Objects.requireNonNull(
                orderBookManager.getOrder(buyEntry.orderId()),
                "Buy order not found in memory"
        );

        Order sellOrder = Objects.requireNonNull(
                orderBookManager.getOrder(sellEntry.orderId()),
                "Sell order not found in memory"
        );

        buyOrder.fillOrderQuantity(executedQuantity);
        sellOrder.fillOrderQuantity(executedQuantity);

        updateStatus(buyOrder);
        updateStatus(sellOrder);

        if (buyOrder.getStatus() == OrderStatus.FILLED) {
            orderBookManager.removeOrderFromOrderBook(buyOrder);
            orderBookManager.unregisterOrder(buyOrder.getId());
        }

        if (sellOrder.getStatus() == OrderStatus.FILLED) {
            orderBookManager.removeOrderFromOrderBook(sellOrder);
            orderBookManager.unregisterOrder(sellOrder.getId());
        }

        if (executedQuantity > 0) {
            orderRepository.save(buyOrder);
            orderRepository.save(sellOrder);
        }

        Fill fillOrder = Fill.builder()
                .buyOrderId(buyOrder.getId())
                .sellOrderId(sellOrder.getId())
                .stockId(buyEntry.stockId())
                .quantity(executedQuantity)
                .price(executionPrice)
                .executedAt(Instant.now())
                .build();

        fillRepository.save(fillOrder);

        log.info("EXECUTED {} @ {} (BUY={}, SELL={})", executedQuantity, executionPrice, buyEntry.orderId(), sellEntry.orderId());
    }

    private void updateStatus(Order order) {
        if (order.getRemainingQuantity() == 0) {
            order.setStatus(OrderStatus.FILLED);
        } else {
            order.setStatus(OrderStatus.PARTIALLY_FILLED);
        }
    }
}
