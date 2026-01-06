package com.siddharth.tradesim_backend.portfolio.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PortfolioHoldingResponse(
        UUID stockId,
        String symbol,
        int quantity,
        BigDecimal currentPrice,
        BigDecimal currentValue
) {
}