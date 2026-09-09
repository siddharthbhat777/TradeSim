package com.siddharth.tradesim_backend.portfolio.model.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PortfolioHoldingResponse(
        UUID stockId,
        String symbol,
        int quantity,
        BigDecimal averageBuyPrice,
        BigDecimal currentPrice,
        BigDecimal currentValue,
        BigDecimal unrealizedPnl,
        BigDecimal nativeAverageBuyPrice,
        BigDecimal nativeCurrentPrice,
        BigDecimal nativeCurrentValue,
        BigDecimal nativeUnrealizedPnl,
        BigDecimal totalInvested,
        String currency,
        BigDecimal fxRate
) {
}