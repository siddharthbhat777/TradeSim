package com.siddharth.tradesim_backend.order.model.dto;

import com.siddharth.tradesim_backend.order.enums.OrderType;
import com.siddharth.tradesim_backend.order.enums.Status;
import com.siddharth.tradesim_backend.order.enums.OrderSide;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TradeResponse(
        UUID tradeId,
        UUID stockId,
        String stockSymbol,
        OrderSide type,
        OrderType orderType,
        Status status,
        int quantity,
        BigDecimal priceAtExecution,
        BigDecimal totalAmount,
        BigDecimal updatedBalance,
        Instant executedAt
) {
}