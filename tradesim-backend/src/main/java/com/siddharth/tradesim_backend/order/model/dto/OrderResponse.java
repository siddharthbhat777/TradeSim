package com.siddharth.tradesim_backend.order.model.dto;

import com.siddharth.tradesim_backend.order.enums.OrderSide;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.enums.OrderType;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        UUID stockId,
        OrderSide side,
        OrderType orderType,
        OrderStatus status,
        int quantity,
        int remainingQuantity,
        BigDecimal limitPrice
) {
}