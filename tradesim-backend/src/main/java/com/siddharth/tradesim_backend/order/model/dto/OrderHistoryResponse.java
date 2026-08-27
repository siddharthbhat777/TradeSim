package com.siddharth.tradesim_backend.order.model.dto;

import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.enums.OrderType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderHistoryResponse(
        UUID orderId,
        UUID stockId,
        String symbol,
        OrderSide side,
        OrderType orderType,
        int quantity,
        int filledQuantity,
        BigDecimal limitPrice,
        OrderStatus status,
        Instant createdAt
) {
}