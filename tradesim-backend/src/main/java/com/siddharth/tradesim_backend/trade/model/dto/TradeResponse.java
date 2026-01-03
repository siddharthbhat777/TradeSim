package com.siddharth.tradesim_backend.trade.model.dto;

import com.siddharth.tradesim_backend.trade.enums.OrderType;
import com.siddharth.tradesim_backend.trade.enums.Status;
import com.siddharth.tradesim_backend.trade.enums.Type;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TradeResponse(
        UUID tradeId,
        UUID stockId,
        String stockSymbol,
        Type type,
        OrderType orderType,
        Status status,
        int quantity,
        BigDecimal priceAtExecution,
        BigDecimal totalAmount,
        BigDecimal updatedBalance,
        Instant executedAt
) {
}