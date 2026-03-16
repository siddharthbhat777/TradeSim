package com.siddharth.tradesim_backend.portfolio.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PortfolioHistoryResponse(
        LocalDate snapshotDate,
        BigDecimal totalValue,
        BigDecimal unrealizedPnl,
        BigDecimal realizedPnl,
        BigDecimal equity
) {
}