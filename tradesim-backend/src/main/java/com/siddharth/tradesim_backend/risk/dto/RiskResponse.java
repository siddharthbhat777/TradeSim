package com.siddharth.tradesim_backend.risk.dto;

import java.math.BigDecimal;

public record RiskResponse(
        BigDecimal equity,
        BigDecimal marginUsed,
        BigDecimal maintenanceMargin,
        BigDecimal unrealizedPnl,
        boolean isUnderLiquidation
) {
}