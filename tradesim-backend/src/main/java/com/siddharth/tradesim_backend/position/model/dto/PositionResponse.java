package com.siddharth.tradesim_backend.position.model.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PositionResponse(
        UUID stockId,
        String symbol,
        int quantity,
        int lockedQuantity,
        BigDecimal averageBuyPrice,
        BigDecimal realizedPnl,
        BigDecimal unrealizedPnl
) {
}