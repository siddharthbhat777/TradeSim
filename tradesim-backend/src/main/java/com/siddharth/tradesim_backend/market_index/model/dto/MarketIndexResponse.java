package com.siddharth.tradesim_backend.market_index.model.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MarketIndexResponse(
        UUID id,
        String name,
        String symbol,
        UUID exchangeId,
        BigDecimal baseValue,
        BigDecimal currentValue,
        BigDecimal change,
        BigDecimal changePercent,
        BigDecimal dayOpen,
        BigDecimal dayHigh,
        BigDecimal dayLow,
        BigDecimal previousClose
) {
}