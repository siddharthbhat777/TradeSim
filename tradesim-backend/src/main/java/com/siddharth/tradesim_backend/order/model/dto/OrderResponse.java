package com.siddharth.tradesim_backend.order.model.dto;

import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.enums.TimeInForce;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        UUID stockId,
        OrderSide side,
        OrderType orderType,
        TimeInForce timeInForce,
        OrderStatus status,
        int quantity,
        int remainingQuantity,
        BigDecimal limitPrice,
        BigDecimal bookPrice,
        Instant expiresAt,
        String haltReason
) {
}