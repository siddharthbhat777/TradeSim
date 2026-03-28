package com.siddharth.tradesim_backend.risk.dto;

import com.siddharth.tradesim_backend.risk.enums.RiskLevel;

import java.math.BigDecimal;

public record RiskResponse(
        BigDecimal equity,
        BigDecimal marginUsed,
        BigDecimal maintenanceMargin,
        BigDecimal unrealizedPnl,
        BigDecimal marginRatio,
        RiskLevel riskLevel,
        boolean isUnderLiquidation
) {
}