package com.siddharth.tradesim_backend.order.orderbook;

import com.siddharth.tradesim_backend.order.enums.OrderSide;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderBookEntry(
        UUID orderId,
        UUID userId,
        UUID stockId,
        OrderSide side,
        BigDecimal price,
        int quantity,
        Instant createdAt
) {
    public OrderBookEntry withReducedQty(int filledQty) {
        return new OrderBookEntry(
                orderId,
                userId,
                stockId,
                side,
                price,
                quantity - filledQty,
                createdAt
        );
    }
}