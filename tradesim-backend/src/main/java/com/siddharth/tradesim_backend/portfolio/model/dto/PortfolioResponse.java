package com.siddharth.tradesim_backend.portfolio.model.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioResponse(
        List<PortfolioHoldingResponse> holdings,
        BigDecimal totalCashValue,
        BigDecimal marginLoan,
        BigDecimal totalValue,
        BigDecimal totalInvested,
        BigDecimal totalUnrealizedPnl,
        BigDecimal totalRealizedPnl,
        BigDecimal totalPnl,
        BigDecimal equity
) {
}