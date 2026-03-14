package com.siddharth.tradesim_backend.portfolio.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioResponse(
        List<PortfolioHoldingResponse> holdings,
        BigDecimal totalValue,
        BigDecimal totalInvested,
        BigDecimal totalUnrealizedPnl,
        BigDecimal totalRealizedPnl,
        BigDecimal totalPnl
) {
}