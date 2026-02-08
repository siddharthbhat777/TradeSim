package com.siddharth.tradesim_backend.order.orderbook;

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
        OrderBook book = orderBookManager.getBook(stockId);

        while (!book.getBuyOrders().isEmpty() && !book.getSellOrders().isEmpty()) {
            OrderBookEntry buy = book.getBuyOrders().peek();
            OrderBookEntry sell = book.getSellOrders().peek();

            log.info("MATCH START stock={}", stockId);
            log.info("BUY={} SELL={}", buy.orderId(), sell.orderId());
            log.info("QTY buy={} sell={}", buy.quantity(), sell.quantity());

            if (buy.price().compareTo(sell.price()) < 0) {
                break;
            }

            int executedQty = Math.min(buy.quantity(), sell.quantity());

            BigDecimal executionPrice = sell.price();

            executeTrade(buy, sell, executedQty, executionPrice);

            book.getBuyOrders().poll();
            book.getSellOrders().poll();

            if (buy.quantity() > executedQty) {
                book.getBuyOrders().add(buy.withReducedQty(executedQty));
            }

            if (sell.quantity() > executedQty) {
                book.getSellOrders().add(sell.withReducedQty(executedQty));
            }
        }
    }

    private void executeTrade(
            OrderBookEntry buy,
            OrderBookEntry sell,
            int qty,
            BigDecimal price
    ) {
        Order buyOrder = orderRepository.findById(buy.orderId()).orElseThrow();
        Order sellOrder = orderRepository.findById(sell.orderId()).orElseThrow();

        buyOrder.fill(qty);
        sellOrder.fill(qty);

        updateStatus(buyOrder);
        updateStatus(sellOrder);

        orderRepository.save(buyOrder);
        orderRepository.save(sellOrder);

        fillRepository.save(
                Fill.builder()
                        .buyOrderId(buyOrder.getId())
                        .sellOrderId(sellOrder.getId())
                        .stockId(buy.stockId())
                        .quantity(qty)
                        .price(price)
                        .executedAt(Instant.now())
                        .build()
        );

        log.info("EXECUTED {} @ {} (BUY={}, SELL={})", qty, price, buy.orderId(), sell.orderId());
    }

    private void updateStatus(Order order) {
        if (order.getRemainingQuantity() == 0) {
            order.setStatus(OrderStatus.FILLED);
        } else {
            order.setStatus(OrderStatus.PARTIALLY_FILLED);
        }
    }
}
